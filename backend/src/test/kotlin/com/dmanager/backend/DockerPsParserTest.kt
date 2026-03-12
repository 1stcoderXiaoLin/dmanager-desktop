package com.dmanager.backend

import com.dmanager.backend.infrastructure.docker.DockerPsParser
import kotlin.test.Test
import kotlin.test.assertEquals

class DockerPsParserTest {
    private val parser = DockerPsParser()

    @Test
    fun `parseLines should parse multiple rows`() {
        val raw =
            """
            {"ID":"id1","Names":"web","Image":"nginx","State":"running","Status":"Up 2 hours"}
            {"ID":"id2","Names":"db","Image":"postgres","State":"exited","Status":"Exited (0) 3 hours ago"}
            """.trimIndent()

        val list = parser.parseLines(raw)

        assertEquals(2, list.size)
        assertEquals("web", list[0].names)
        assertEquals("postgres", list[1].image)
    }
}
