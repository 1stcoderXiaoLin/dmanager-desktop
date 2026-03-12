package com.dmanager.backend.api

import com.dmanager.backend.domain.SshAuthType
import kotlinx.serialization.Serializable

@Serializable
data class CreateHostRequest(
    val name: String,
    val address: String,
    val port: Int = 22,
    val username: String,
    val authType: SshAuthType,
    val password: String? = null,
    val privateKeyPath: String? = null,
    val privateKeyPassphrase: String? = null,
)

@Serializable
data class HostResponse(
    val id: String,
    val name: String,
    val address: String,
    val port: Int,
    val username: String,
    val authType: SshAuthType,
    val privateKeyPath: String? = null,
)

@Serializable
data class CommandRequest(
    val command: String,
)

@Serializable
data class CommandResponse(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

@Serializable
data class ContainerSummaryResponse(
    val id: String,
    val names: String,
    val image: String,
    val state: String,
    val status: String,
)

@Serializable
data class DeleteResponse(
    val success: Boolean,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class ApiError(
    val message: String,
)
