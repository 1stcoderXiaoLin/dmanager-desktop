package com.dmanager.backend.application

import com.dmanager.backend.domain.CommandResult
import com.dmanager.backend.domain.DeleteResult
import com.dmanager.backend.domain.DockerRuntimeAdapter
import com.dmanager.backend.domain.DomainException
import com.dmanager.backend.domain.HostRepository
import java.io.Closeable

/**
 * 容器管理服务。
 *
 * 深度删除策略：
 * 1. 先读取容器挂载的 named volume
 * 2. 执行 docker rm -f -v 删除容器与匿名卷
 * 3. best-effort 删除 named volume（失败只写 warning，不回滚主删除）
 */
class ContainerService(
    private val hostRepository: HostRepository,
    private val dockerRuntimeAdapter: DockerRuntimeAdapter,
) {
    fun list(hostId: String) = dockerRuntimeAdapter.listContainers(getHost(hostId))

    fun start(
        hostId: String,
        containerId: String,
    ): CommandResult {
        if (containerId.isBlank()) throw DomainException("containerId 不能为空")
        return dockerRuntimeAdapter.startContainer(getHost(hostId), containerId)
    }

    fun remove(
        hostId: String,
        containerId: String,
    ): DeleteResult {
        val result = dockerRuntimeAdapter.removeContainer(getHost(hostId), containerId, deep = false)
        if (result.exitCode != 0) {
            throw DomainException("普通删除失败：${result.stderr.ifBlank { result.stdout }}")
        }
        return DeleteResult(success = true)
    }

    fun deepRemove(
        hostId: String,
        containerId: String,
    ): DeleteResult {
        val host = getHost(hostId)

        // 先取出 named volume，用于后置清理。
        val namedVolumes = dockerRuntimeAdapter.listAttachedNamedVolumes(host, containerId)

        val removeResult = dockerRuntimeAdapter.removeContainer(host, containerId, deep = true)
        if (removeResult.exitCode != 0) {
            throw DomainException("深度删除失败：${removeResult.stderr.ifBlank { removeResult.stdout }}")
        }

        val warnings = mutableListOf<String>()
        namedVolumes.forEach { volume ->
            val volumeResult = dockerRuntimeAdapter.removeVolume(host, volume)
            if (volumeResult.exitCode != 0) {
                warnings += "卷 $volume 清理失败：${volumeResult.stderr.ifBlank { volumeResult.stdout }}"
            }
        }

        return DeleteResult(success = true, warnings = warnings)
    }

    fun runCommand(
        hostId: String,
        containerId: String,
        command: String,
    ): CommandResult {
        if (command.isBlank()) throw DomainException("命令不能为空")
        return dockerRuntimeAdapter.runContainerCommand(getHost(hostId), containerId, command)
    }

    fun streamLogs(
        hostId: String,
        containerId: String,
        onLine: (String) -> Unit,
    ): Closeable {
        return dockerRuntimeAdapter.streamContainerLogs(getHost(hostId), containerId, onLine)
    }

    private fun getHost(hostId: String) =
        hostRepository.findById(hostId)
            ?: throw DomainException("主机不存在：$hostId")
}
