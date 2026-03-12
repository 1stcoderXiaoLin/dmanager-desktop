package com.dmanager.backend

import com.dmanager.backend.application.CreateHostCommand
import com.dmanager.backend.application.HostService
import com.dmanager.backend.domain.CommandResult
import com.dmanager.backend.domain.ContainerSummary
import com.dmanager.backend.domain.CredentialStore
import com.dmanager.backend.domain.DockerRuntimeAdapter
import com.dmanager.backend.domain.HostRepository
import com.dmanager.backend.domain.RemoteHost
import com.dmanager.backend.domain.SshAuthType
import java.io.Closeable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HostServiceTest {
    @Test
    fun `create password host should save password into credential store`() {
        val hostRepo = TestHostRepository()
        val credentialStore = TestCredentialStore()
        val service = HostService(hostRepo, credentialStore, NoopDockerAdapter())

        val host =
            service.create(
                CreateHostCommand(
                    name = "dev",
                    address = "127.0.0.1",
                    username = "root",
                    authType = SshAuthType.PASSWORD,
                    password = "123456",
                ),
            )

        assertNotNull(host.id)
        assertEquals("123456", credentialStore.read(host.id, "password"))
    }

    @Test
    fun `list should return saved hosts`() {
        val hostRepo = TestHostRepository()
        val credentialStore = TestCredentialStore()
        val service = HostService(hostRepo, credentialStore, NoopDockerAdapter())

        service.create(
            CreateHostCommand(
                name = "a-host",
                address = "10.0.0.2",
                username = "root",
                authType = SshAuthType.PASSWORD,
                password = "pw",
            ),
        )

        val list = service.list()
        assertEquals(1, list.size)
        assertEquals("a-host", list.first().name)
    }

    @Test
    fun `delete should remove host and secret`() {
        val hostRepo = TestHostRepository()
        val credentialStore = TestCredentialStore()
        val service = HostService(hostRepo, credentialStore, NoopDockerAdapter())

        val host =
            service.create(
                CreateHostCommand(
                    name = "demo",
                    address = "10.0.0.3",
                    username = "root",
                    authType = SshAuthType.PASSWORD,
                    password = "pw",
                ),
            )
        val deleted = service.delete(host.id)

        assertTrue(deleted)
        assertEquals(null, credentialStore.read(host.id, "password"))
    }
}

private class TestHostRepository : HostRepository {
    private val data = linkedMapOf<String, RemoteHost>()

    override fun save(host: RemoteHost) {
        data[host.id] = host
    }

    override fun findById(id: String): RemoteHost? = data[id]

    override fun list(): List<RemoteHost> = data.values.toList()

    override fun delete(id: String): Boolean = data.remove(id) != null
}

private class TestCredentialStore : CredentialStore {
    private val data = mutableMapOf<String, String>()

    override fun save(
        hostId: String,
        key: String,
        value: String,
    ) {
        data["$hostId::$key"] = value
    }

    override fun read(
        hostId: String,
        key: String,
    ): String? = data["$hostId::$key"]

    override fun delete(
        hostId: String,
        key: String,
    ) {
        data.remove("$hostId::$key")
    }
}

private class NoopDockerAdapter : DockerRuntimeAdapter {
    override fun ping(host: RemoteHost): CommandResult = CommandResult(0, "ok", "")

    override fun listContainers(host: RemoteHost): List<ContainerSummary> = emptyList()

    override fun startContainer(
        host: RemoteHost,
        containerId: String,
    ): CommandResult = CommandResult(0, "", "")

    override fun removeContainer(
        host: RemoteHost,
        containerId: String,
        deep: Boolean,
    ): CommandResult {
        return CommandResult(0, "", "")
    }

    override fun listAttachedNamedVolumes(
        host: RemoteHost,
        containerId: String,
    ): List<String> = emptyList()

    override fun removeVolume(
        host: RemoteHost,
        volumeName: String,
    ): CommandResult = CommandResult(0, "", "")

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
