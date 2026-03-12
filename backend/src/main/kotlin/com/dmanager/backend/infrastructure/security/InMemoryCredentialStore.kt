package com.dmanager.backend.infrastructure.security

import com.dmanager.backend.domain.CredentialStore
import java.util.concurrent.ConcurrentHashMap

/**
 * 内存版凭据存储。
 *
 * 重要说明：
 * - 该实现只用于开发与测试，不会持久化到磁盘。
 * - 生产环境应替换为系统密钥链（Windows Credential Manager / Keychain / libsecret）。
 */
class InMemoryCredentialStore : CredentialStore {
    private val secrets = ConcurrentHashMap<String, String>()

    override fun save(
        hostId: String,
        key: String,
        value: String,
    ) {
        secrets[buildKey(hostId, key)] = value
    }

    override fun read(
        hostId: String,
        key: String,
    ): String? = secrets[buildKey(hostId, key)]

    override fun delete(
        hostId: String,
        key: String,
    ) {
        secrets.remove(buildKey(hostId, key))
    }

    private fun buildKey(
        hostId: String,
        key: String,
    ): String = "$hostId::$key"
}
