package com.dmanager.backend.infrastructure.docker

import com.dmanager.backend.domain.CommandResult
import com.dmanager.backend.domain.ContainerSummary
import com.dmanager.backend.domain.DockerRuntimeAdapter
import com.dmanager.backend.domain.DomainException
import com.dmanager.backend.domain.RemoteHost
import com.dmanager.backend.infrastructure.ssh.CommandExecutor
import java.io.Closeable

/**
 * 基于 SSH + Docker CLI 的运行时适配器。
 *
 * 可扩展点：
 * - 后续如果改成 Docker HTTP API，可新增实现并替换注入，不影响上层业务服务。
 */
class SshDockerRuntimeAdapter(
    private val executor: CommandExecutor,
    private val commandFactory: DockerCommandFactory,
    private val parser: DockerPsParser,
) : DockerRuntimeAdapter {
    override fun ping(host: RemoteHost): CommandResult {
        return executeOrThrow(host, commandFactory.ping(), "检测 Docker 服务失败")
    }

    override fun listContainers(host: RemoteHost): List<ContainerSummary> {
        val result = executeOrThrow(host, commandFactory.listContainers(), "拉取容器列表失败")
        return parser.parseLines(result.stdout)
    }

    override fun startContainer(
        host: RemoteHost,
        containerId: String,
    ): CommandResult {
        return executeOrThrow(host, commandFactory.startContainer(containerId), "启动容器失败")
    }

    override fun removeContainer(
        host: RemoteHost,
        containerId: String,
        deep: Boolean,
    ): CommandResult {
        return executeOrThrow(host, commandFactory.removeContainer(containerId, deep), "删除容器失败")
    }

    override fun listAttachedNamedVolumes(
        host: RemoteHost,
        containerId: String,
    ): List<String> {
        val result = executeOrThrow(host, commandFactory.inspectNamedVolumes(containerId), "查询容器卷失败")
        return result.stdout
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    override fun removeVolume(
        host: RemoteHost,
        volumeName: String,
    ): CommandResult {
        return executor.execute(host, commandFactory.removeVolume(volumeName))
    }

    override fun runContainerCommand(
        host: RemoteHost,
        containerId: String,
        command: String,
    ): CommandResult {
        return executor.execute(host, commandFactory.runContainerCommand(containerId, command), timeoutSeconds = 120)
    }

    override fun streamContainerLogs(
        host: RemoteHost,
        containerId: String,
        onLine: (String) -> Unit,
    ): Closeable {
        return executor.executeStreaming(host, commandFactory.streamLogs(containerId), onLine)
    }

    private fun executeOrThrow(
        host: RemoteHost,
        command: String,
        operation: String,
    ): CommandResult {
        val result = executor.execute(host, command)
        if (result.exitCode != 0) {
            val message =
                buildString {
                    append("$operation: ")
                    append(result.stderr.ifBlank { result.stdout })
                }
            throw DomainException(message)
        }
        return result
    }
}
