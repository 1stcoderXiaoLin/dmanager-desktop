package com.dmanager.backend.domain

import java.io.Closeable

/**
 * SSH 认证方式。
 *
 * 示例：
 * - PASSWORD: 使用用户名 + 密码
 * - PRIVATE_KEY: 使用用户名 + 私钥路径 (+ 可选口令)
 */
enum class SshAuthType {
    PASSWORD,
    PRIVATE_KEY,
}

/**
 * 远程主机基础信息。
 *
 * 注意：该对象只保存非敏感字段。
 * 密码、私钥口令等敏感信息必须放到 CredentialStore。
 */
data class RemoteHost(
    val id: String,
    val name: String,
    val address: String,
    val port: Int,
    val username: String,
    val authType: SshAuthType,
    val privateKeyPath: String? = null,
)

/**
 * 容器列表项（从 docker ps 解析）。
 */
data class ContainerSummary(
    val id: String,
    val names: String,
    val image: String,
    val state: String,
    val status: String,
)

/**
 * 命令执行结果。
 */
data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

/**
 * 删除结果。
 * warnings 用于返回“主操作成功，但部分残留清理失败”的信息。
 */
data class DeleteResult(
    val success: Boolean,
    val warnings: List<String> = emptyList(),
)

class DomainException(message: String) : RuntimeException(message)

interface HostRepository {
    fun save(host: RemoteHost)

    fun findById(id: String): RemoteHost?

    fun list(): List<RemoteHost>

    fun delete(id: String): Boolean
}

interface CredentialStore {
    fun save(
        hostId: String,
        key: String,
        value: String,
    )

    fun read(
        hostId: String,
        key: String,
    ): String?

    fun delete(
        hostId: String,
        key: String,
    )
}

interface DockerRuntimeAdapter {
    fun ping(host: RemoteHost): CommandResult

    fun listContainers(host: RemoteHost): List<ContainerSummary>

    fun startContainer(
        host: RemoteHost,
        containerId: String,
    ): CommandResult

    fun removeContainer(
        host: RemoteHost,
        containerId: String,
        deep: Boolean,
    ): CommandResult

    fun listAttachedNamedVolumes(
        host: RemoteHost,
        containerId: String,
    ): List<String>

    fun removeVolume(
        host: RemoteHost,
        volumeName: String,
    ): CommandResult

    fun runContainerCommand(
        host: RemoteHost,
        containerId: String,
        command: String,
    ): CommandResult

    fun streamContainerLogs(
        host: RemoteHost,
        containerId: String,
        onLine: (String) -> Unit,
    ): Closeable
}
