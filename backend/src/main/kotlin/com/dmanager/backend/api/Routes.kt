package com.dmanager.backend.api

import com.dmanager.backend.application.ContainerService
import com.dmanager.backend.application.CreateHostCommand
import com.dmanager.backend.application.HostService
import com.dmanager.backend.domain.DomainException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference

fun Application.registerRoutes(
    hostService: HostService,
    containerService: ContainerService,
) {
    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        route("/api") {
            route("/hosts") {
                get {
                    val hosts =
                        hostService.list().map {
                            HostResponse(
                                id = it.id,
                                name = it.name,
                                address = it.address,
                                port = it.port,
                                username = it.username,
                                authType = it.authType,
                                privateKeyPath = it.privateKeyPath,
                            )
                        }
                    call.respond(hosts)
                }

                post {
                    runCatching {
                        val req = call.receive<CreateHostRequest>()
                        val created =
                            hostService.create(
                                CreateHostCommand(
                                    name = req.name,
                                    address = req.address,
                                    port = req.port,
                                    username = req.username,
                                    authType = req.authType,
                                    password = req.password,
                                    privateKeyPath = req.privateKeyPath,
                                    privateKeyPassphrase = req.privateKeyPassphrase,
                                ),
                            )
                        call.respond(
                            HostResponse(
                                id = created.id,
                                name = created.name,
                                address = created.address,
                                port = created.port,
                                username = created.username,
                                authType = created.authType,
                                privateKeyPath = created.privateKeyPath,
                            ),
                        )
                    }.onFailure { call.respondError(it) }
                }

                delete("/{hostId}") {
                    runCatching {
                        val hostId = call.parameters["hostId"].orEmpty()
                        val deleted = hostService.delete(hostId)
                        if (!deleted) {
                            call.respond(HttpStatusCode.NotFound, ApiError("Host not found: $hostId"))
                            return@runCatching
                        }
                        call.respond(HttpStatusCode.NoContent)
                    }.onFailure { call.respondError(it) }
                }

                post("/{hostId}/test") {
                    runCatching {
                        val hostId = call.parameters["hostId"].orEmpty()
                        hostService.testConnection(hostId)
                        call.respond(mapOf("ok" to true))
                    }.onFailure { call.respondError(it) }
                }

                get("/{hostId}/containers") {
                    runCatching {
                        val hostId = call.parameters["hostId"].orEmpty()
                        val containers =
                            containerService.list(hostId).map {
                                ContainerSummaryResponse(
                                    id = it.id,
                                    names = it.names,
                                    image = it.image,
                                    state = it.state,
                                    status = it.status,
                                )
                            }
                        call.respond(containers)
                    }.onFailure { call.respondError(it) }
                }

                post("/{hostId}/containers/{containerId}/start") {
                    runCatching {
                        val hostId = call.parameters["hostId"].orEmpty()
                        val containerId = call.parameters["containerId"].orEmpty()
                        val result = containerService.start(hostId, containerId)
                        call.respond(CommandResponse(result.exitCode, result.stdout, result.stderr))
                    }.onFailure { call.respondError(it) }
                }

                delete("/{hostId}/containers/{containerId}") {
                    runCatching {
                        val hostId = call.parameters["hostId"].orEmpty()
                        val containerId = call.parameters["containerId"].orEmpty()
                        val result = containerService.remove(hostId, containerId)
                        call.respond(DeleteResponse(result.success, result.warnings))
                    }.onFailure { call.respondError(it) }
                }

                delete("/{hostId}/containers/{containerId}/deep") {
                    runCatching {
                        val hostId = call.parameters["hostId"].orEmpty()
                        val containerId = call.parameters["containerId"].orEmpty()
                        val result = containerService.deepRemove(hostId, containerId)
                        call.respond(DeleteResponse(result.success, result.warnings))
                    }.onFailure { call.respondError(it) }
                }
            }

            webSocket("/ws/hosts/{hostId}/containers/{containerId}/exec") {
                val hostId = call.parameters["hostId"].orEmpty()
                val containerId = call.parameters["containerId"].orEmpty()

                send(Frame.Text("Shell session connected. Enter a single-line command such as: ls -la /"))

                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        runCatching {
                            val command = frame.readText()
                            val result = containerService.runCommand(hostId, containerId, command)
                            send(Frame.Text(result.stdout.ifBlank { "(no output)" }))
                            if (result.stderr.isNotBlank()) {
                                send(Frame.Text("[stderr] ${result.stderr}"))
                            }
                            if (result.exitCode != 0) {
                                send(Frame.Text("[exitCode] ${result.exitCode}"))
                            }
                        }.onFailure {
                            send(Frame.Text("[error] ${it.message}"))
                        }
                    }
                }
            }

            webSocket("/ws/hosts/{hostId}/containers/{containerId}/logs") {
                val hostId = call.parameters["hostId"].orEmpty()
                val containerId = call.parameters["containerId"].orEmpty()
                val closer = AtomicReference<Closeable?>()

                runCatching {
                    val handle =
                        containerService.streamLogs(hostId, containerId) { line ->
                            launch(Dispatchers.IO) {
                                send(Frame.Text(line))
                            }
                        }
                    closer.set(handle)
                }.onFailure {
                    send(Frame.Text("[error] ${it.message}"))
                }

                try {
                    incoming.consumeEach { }
                } finally {
                    closer.get()?.close()
                }
            }
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.respondError(throwable: Throwable) {
    val message = throwable.message ?: "Unknown error"
    val status =
        if (throwable is DomainException) {
            HttpStatusCode.BadRequest
        } else {
            HttpStatusCode.InternalServerError
        }
    respond(status, ApiError(message))
}
