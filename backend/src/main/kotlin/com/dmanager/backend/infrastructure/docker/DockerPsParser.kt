package com.dmanager.backend.infrastructure.docker

import com.dmanager.backend.domain.ContainerSummary
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 解析 docker ps --format '{{json .}}' 的逐行 JSON 输出。
 */
class DockerPsParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseLines(raw: String): List<ContainerSummary> {
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                val obj = json.parseToJsonElement(line).jsonObject
                ContainerSummary(
                    id = obj["ID"]?.jsonPrimitive?.content.orEmpty(),
                    names = obj["Names"]?.jsonPrimitive?.content.orEmpty(),
                    image = obj["Image"]?.jsonPrimitive?.content.orEmpty(),
                    state = obj["State"]?.jsonPrimitive?.content.orEmpty(),
                    status = obj["Status"]?.jsonPrimitive?.content.orEmpty(),
                )
            }
            .toList()
    }
}
