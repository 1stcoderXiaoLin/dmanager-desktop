package com.dmanager.backend.application

import com.dmanager.backend.domain.CredentialStore
import com.dmanager.backend.domain.DockerRuntimeAdapter
import com.dmanager.backend.domain.DomainException
import com.dmanager.backend.domain.HostRepository
import com.dmanager.backend.domain.RemoteHost
import com.dmanager.backend.domain.SshAuthType
import com.dmanager.backend.infrastructure.ssh.SshCommandExecutor
import java.util.UUID

data class CreateHostCommand(
    val name: String,
    val address: String,
    val port: Int = 22,
    val username: String,
    val authType: SshAuthType,
    val password: String? = null,
    val privateKeyPath: String? = null,
    val privateKeyPassphrase: String? = null,
)

/**
 * 主机管理服务。
 *
 * 设计原则：
 * - 只保存“可公开配置”的主机信息。
 * - 敏感字段进入 CredentialStore。
 */
class HostService(
    private val hostRepository: HostRepository,
    private val credentialStore: CredentialStore,
    private val dockerRuntimeAdapter: DockerRuntimeAdapter,
) {
    fun create(command: CreateHostCommand): RemoteHost {
        validateCreateCommand(command)

        val host =
            RemoteHost(
                id = UUID.randomUUID().toString(),
                name = command.name,
                address = command.address,
                port = command.port,
                username = command.username,
                authType = command.authType,
                privateKeyPath = command.privateKeyPath,
            )
        hostRepository.save(host)

        when (command.authType) {
            SshAuthType.PASSWORD -> {
                credentialStore.save(host.id, SshCommandExecutor.KEY_PASSWORD, command.password!!.trim())
            }

            SshAuthType.PRIVATE_KEY -> {
                val passphrase = command.privateKeyPassphrase
                if (!passphrase.isNullOrBlank()) {
                    credentialStore.save(host.id, SshCommandExecutor.KEY_PRIVATE_KEY_PASSPHRASE, passphrase)
                }
            }
        }

        return host
    }

    fun list(): List<RemoteHost> = hostRepository.list()

    fun delete(hostId: String): Boolean {
        credentialStore.delete(hostId, SshCommandExecutor.KEY_PASSWORD)
        credentialStore.delete(hostId, SshCommandExecutor.KEY_PRIVATE_KEY_PASSPHRASE)
        return hostRepository.delete(hostId)
    }

    fun testConnection(hostId: String) {
        val host = getRequiredHost(hostId)
        val ping = dockerRuntimeAdapter.ping(host)
        if (ping.exitCode != 0) {
            throw DomainException("连接测试失败：${ping.stderr.ifBlank { ping.stdout }}")
        }
    }

    fun getRequiredHost(hostId: String): RemoteHost {
        return hostRepository.findById(hostId)
            ?: throw DomainException("主机不存在：$hostId")
    }

    private fun validateCreateCommand(command: CreateHostCommand) {
        if (command.name.isBlank()) throw DomainException("主机名称不能为空")
        if (command.address.isBlank()) throw DomainException("主机地址不能为空")
        if (command.username.isBlank()) throw DomainException("SSH 用户名不能为空")
        if (command.port !in 1..65535) throw DomainException("端口范围必须在 1~65535")

        when (command.authType) {
            SshAuthType.PASSWORD -> {
                if (command.password.isNullOrBlank()) {
                    throw DomainException("密码认证时，必须提供 password")
                }
            }

            SshAuthType.PRIVATE_KEY -> {
                if (command.privateKeyPath.isNullOrBlank()) {
                    throw DomainException("私钥认证时，必须提供 privateKeyPath")
                }
            }
        }
    }
}
