package com.dmanager.backend

import com.dmanager.backend.application.ContainerService
import com.dmanager.backend.domain.CommandResult
import com.dmanager.backend.domain.ContainerSummary
import com.dmanager.backend.domain.DockerRuntimeAdapter
import com.dmanager.backend.domain.HostRepository
import com.dmanager.backend.domain.RemoteHost
import com.dmanager.backend.domain.SshAuthType
import java.io.Closeable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContainerServiceTest {
    @Test
    fun `deepRemove should report warning when volume cleanup fails`() {
        val host =
            RemoteHost(
                id = "h1",
                name = "dev",
                address = "127.0.0.1",
                port = 22,
                username = "root",
                authType = SshAuthType.PASSWORD,
            )
        val repo = FakeHostRepository(host)
        val adapter =
            FakeDockerRuntimeAdapter(
                removeContainerResult = CommandResult(0, "", ""),
                volumeResults =
                    mapOf(
                        "keep_volume" to CommandResult(1, "", "in use"),
                    ),
            )

        val service = ContainerService(repo, adapter)
        val result = service.deepRemove("h1", "c1")

        assertTrue(result.success)
        assertEquals(1, result.warnings.size)
        assertTrue(result.warnings.first().contains("keep_volume"))
    }

    @Test
    fun `remove should succeed when docker returns zero`() {
        val host =
            RemoteHost(
                id = "h1",
                name = "dev",
                address = "127.0.0.1",
                port = 22,
                username = "root",
                authType = SshAuthType.PASSWORD,
            )
        val repo = FakeHostRepository(host)
        val adapter = FakeDockerRuntimeAdapter(removeContainerResult = CommandResult(0, "ok", ""))

        val service = ContainerService(repo, adapter)
        val result = service.remove("h1", "c1")

        assertTrue(result.success)
    }
}

private class FakeHostRepository(private val host: RemoteHost) : HostRepository {
    override fun save(host: RemoteHost) = Unit

    override fun findById(id: String): RemoteHost? = if (id == host.id) host else null

    override fun list(): List<RemoteHost> = listOf(host)

    override fun delete(id: String): Boolean = false
}

private class FakeDockerRuntimeAdapter(
    private val removeContainerResult: CommandResult = CommandResult(0, "", ""),
    private val volumeResults: Map<String, CommandResult> = emptyMap(),
) : DockerRuntimeAdapter {
    override fun ping(host: RemoteHost): CommandResult = CommandResult(0, "", "")

    override fun listContainers(host: RemoteHost): List<ContainerSummary> = emptyList()

    override fun startContainer(
        host: RemoteHost,
        containerId: String,
    ): CommandResult = CommandResult(0, "", "")

    override fun removeContainer(
        host: RemoteHost,
        containerId: String,
        deep: Boolean,
    ): CommandResult = removeContainerResult

    override fun listAttachedNamedVolumes(
        host: RemoteHost,
        containerId: String,
    ): List<String> = listOf("keep_volume")

    override fun removeVolume(
        host: RemoteHost,
        volumeName: String,
    ): CommandResult {
        return volumeResults[volumeName] ?: CommandResult(0, "", "")
    }

    override fun runContainerCommand(
        host: RemoteHost,
        containerId: String,
        command: String,
    ): CommandResult {
        return CommandResult(0, "", "")
    }

    override fun streamContainerLogs(
        host: RemoteHost,
        containerId: String,
        onLine: (String) -> Unit,
    ): Closeable {
        return Closeable { }
    }
}
