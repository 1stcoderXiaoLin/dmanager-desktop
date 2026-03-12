package com.dmanager.backend.infrastructure.host

import com.dmanager.backend.domain.HostRepository
import com.dmanager.backend.domain.RemoteHost
import java.util.concurrent.ConcurrentHashMap

/**
 * 内存主机仓库。
 *
 * 说明：
 * - 便于本地开发和单元测试。
 * - 生产环境可替换为 SQLite/PostgreSQL 实现。
 */
class InMemoryHostRepository : HostRepository {
    private val data = ConcurrentHashMap<String, RemoteHost>()

    override fun save(host: RemoteHost) {
        data[host.id] = host
    }

    override fun findById(id: String): RemoteHost? = data[id]

    override fun list(): List<RemoteHost> = data.values.sortedBy { it.name.lowercase() }

    override fun delete(id: String): Boolean = data.remove(id) != null
}
