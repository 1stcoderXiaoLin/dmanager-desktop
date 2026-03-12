package com.dmanager.backend

import com.dmanager.backend.infrastructure.docker.DockerCommandFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DockerCommandFactoryTest {
    private val factory = DockerCommandFactory()

    @Test
    fun `quote should escape single quote safely`() {
        val quoted = factory.quote("abc'def")
        assertEquals("'abc'\"'\"'def'", quoted)
    }

    @Test
    fun `remove deep should include force and volume flags`() {
        val command = factory.removeContainer("container-1", deep = true)
        assertTrue(command.contains("docker rm -f -v"))
    }

    @Test
    fun `remove normal should not include deep flags`() {
        val command = factory.removeContainer("container-1", deep = false)
        assertEquals("docker rm 'container-1'", command)
    }
}
