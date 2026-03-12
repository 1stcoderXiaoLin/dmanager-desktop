package com.dmanager.backend

import com.dmanager.backend.api.registerRoutes
import com.dmanager.backend.application.ContainerService
import com.dmanager.backend.application.HostService
import com.dmanager.backend.infrastructure.docker.DockerCommandFactory
import com.dmanager.backend.infrastructure.docker.DockerPsParser
import com.dmanager.backend.infrastructure.docker.SshDockerRuntimeAdapter
import com.dmanager.backend.infrastructure.host.InMemoryHostRepository
import com.dmanager.backend.infrastructure.security.InMemoryCredentialStore
import com.dmanager.backend.infrastructure.ssh.SshCommandExecutor
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.websocket.WebSockets

private const val DEFAULT_BACKEND_HOST = "127.0.0.1"
private const val DEFAULT_BACKEND_PORT = 18080

fun main() {
    embeddedServer(
        Netty,
        port = readBackendPort(),
        host = System.getenv("DMANAGER_BACKEND_HOST")?.ifBlank { null } ?: DEFAULT_BACKEND_HOST,
    ) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    install(CORS) {
        allowHost("127.0.0.1:1420", schemes = listOf("http"))
        allowHost("localhost:1420", schemes = listOf("http"))
        allowHost("127.0.0.1:5173", schemes = listOf("http"))
        allowHost("localhost:5173", schemes = listOf("http"))
        allowHost("tauri.localhost", schemes = listOf("http", "https"))
        allowHost("localhost", schemes = listOf("tauri"))
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowNonSimpleContentTypes = true
    }

    install(WebSockets)

    val hostRepo = InMemoryHostRepository()
    val credentialStore = InMemoryCredentialStore()
    val commandExecutor = SshCommandExecutor(credentialStore)
    val adapter =
        SshDockerRuntimeAdapter(
            executor = commandExecutor,
            commandFactory = DockerCommandFactory(),
            parser = DockerPsParser(),
        )

    val hostService = HostService(hostRepo, credentialStore, adapter)
    val containerService = ContainerService(hostRepo, adapter)

    registerRoutes(hostService, containerService)
}

private fun readBackendPort(): Int {
    val rawPort = System.getenv("DMANAGER_BACKEND_PORT")?.trim().orEmpty()
    return rawPort.toIntOrNull()?.takeIf { it in 1..65535 } ?: DEFAULT_BACKEND_PORT
}
