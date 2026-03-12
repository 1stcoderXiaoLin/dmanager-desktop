package com.dmanager.backend.infrastructure.docker

/**
 * 统一维护 Docker CLI 命令模板。
 *
 * 设计原因：
 * - 将命令拼装集中到一个类，避免在业务逻辑里散落字符串。
 * - 便于后续替换为 Docker Remote API 适配器。
 */
class DockerCommandFactory {
    fun ping(): String = "docker version --format '{{.Server.Version}}'"

    fun listContainers(): String = "docker ps -a --format '{{json .}}'"

    fun startContainer(containerId: String): String = "docker start ${quote(containerId)}"

    fun removeContainer(
        containerId: String,
        deep: Boolean,
    ): String {
        return if (deep) {
            "docker rm -f -v ${quote(containerId)}"
        } else {
            "docker rm ${quote(containerId)}"
        }
    }

    fun inspectNamedVolumes(containerId: String): String {
        return "docker inspect --format '{{range .Mounts}}{{if eq .Type \"volume\"}}{{.Name}}{{println}}{{end}}{{end}}' ${quote(
            containerId,
        )}"
    }

    fun removeVolume(volumeName: String): String = "docker volume rm ${quote(volumeName)}"

    fun runContainerCommand(
        containerId: String,
        command: String,
    ): String {
        // 示例：docker exec <id> sh -lc 'ls -la /app'
        return "docker exec ${quote(containerId)} sh -lc ${quote(command)}"
    }

    fun streamLogs(containerId: String): String = "docker logs -f --tail 200 ${quote(containerId)}"

    /**
     * 使用单引号进行 shell 转义。
     *
     * 示例：
     * 输入：abc'def
     * 输出：'abc'"'"'def'
     */
    fun quote(raw: String): String = "'${raw.replace("'", "'\"'\"'")}'"
}
