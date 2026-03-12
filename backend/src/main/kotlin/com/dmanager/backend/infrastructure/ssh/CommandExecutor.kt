package com.dmanager.backend.infrastructure.ssh

import com.dmanager.backend.domain.CommandResult
import com.dmanager.backend.domain.CredentialStore
import com.dmanager.backend.domain.DomainException
import com.dmanager.backend.domain.RemoteHost
import com.dmanager.backend.domain.SshAuthType
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

interface CommandExecutor {
    fun execute(
        host: RemoteHost,
        command: String,
        timeoutSeconds: Long = 30,
    ): CommandResult

    fun executeStreaming(
        host: RemoteHost,
        command: String,
        onLine: (String) -> Unit,
    ): Closeable
}

/**
 * SSH 命令执行器。
 *
 * 说明：
 * - 通过 CredentialStore 动态读取敏感信息，避免把密码放进持久化配置。
 * - 当前示例使用 PromiscuousVerifier 以便开发环境快速联调。
 *   生产环境建议改为 known_hosts 校验。
 */
class SshCommandExecutor(
    private val credentialStore: CredentialStore,
) : CommandExecutor {
    override fun execute(
        host: RemoteHost,
        command: String,
        timeoutSeconds: Long,
    ): CommandResult {
        val ssh = createAndAuthClient(host)
        ssh.use { client ->
            client.startSession().use { session ->
                val cmd = session.exec(command)
                cmd.join(timeoutSeconds, TimeUnit.SECONDS)
                val stdout = cmd.inputStream.readBytes().toString(StandardCharsets.UTF_8)
                val stderr = cmd.errorStream.readBytes().toString(StandardCharsets.UTF_8)
                val exitCode = cmd.exitStatus ?: -1
                return CommandResult(exitCode = exitCode, stdout = stdout, stderr = stderr)
            }
        }
    }

    override fun executeStreaming(
        host: RemoteHost,
        command: String,
        onLine: (String) -> Unit,
    ): Closeable {
        val ssh = createAndAuthClient(host)
        val session = ssh.startSession()
        val cmd = session.exec(command)

        val outReader = BufferedReader(InputStreamReader(cmd.inputStream, StandardCharsets.UTF_8))
        val errReader = BufferedReader(InputStreamReader(cmd.errorStream, StandardCharsets.UTF_8))

        val outThread =
            Thread {
                outReader.lineSequence().forEach(onLine)
            }
        val errThread =
            Thread {
                errReader.lineSequence().forEach { onLine("[stderr] $it") }
            }

        outThread.isDaemon = true
        errThread.isDaemon = true
        outThread.start()
        errThread.start()

        return Closeable {
            try {
                cmd.close()
            } finally {
                session.close()
                ssh.disconnect()
                ssh.close()
            }
        }
    }

    private fun createAndAuthClient(host: RemoteHost): SSHClient {
        val ssh = SSHClient()
        ssh.addHostKeyVerifier(PromiscuousVerifier())
        ssh.connect(host.address, host.port)

        when (host.authType) {
            SshAuthType.PASSWORD -> {
                val password =
                    credentialStore.read(host.id, KEY_PASSWORD)
                        ?: throw DomainException("主机 ${host.name} 缺少 SSH 密码。")
                ssh.authPassword(host.username, password)
            }

            SshAuthType.PRIVATE_KEY -> {
                val keyPath =
                    host.privateKeyPath
                        ?: throw DomainException("主机 ${host.name} 未设置私钥路径。")
                val passphrase = credentialStore.read(host.id, KEY_PRIVATE_KEY_PASSPHRASE)
                val keyProvider =
                    if (passphrase.isNullOrBlank()) {
                        ssh.loadKeys(keyPath)
                    } else {
                        ssh.loadKeys(keyPath, passphrase)
                    }
                ssh.authPublickey(host.username, keyProvider)
            }
        }
        return ssh
    }

    companion object {
        const val KEY_PASSWORD = "password"
        const val KEY_PRIVATE_KEY_PASSPHRASE = "private_key_passphrase"
    }
}
