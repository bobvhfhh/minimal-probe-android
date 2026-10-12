package com.minprobe.app.data.repository

import android.util.Log
import com.minprobe.app.data.local.crypto.CryptoManager
import com.minprobe.app.data.local.dao.ServerInstanceDao
import com.minprobe.app.data.local.entity.ServerInstanceEntity
import com.minprobe.app.data.remote.AuthException
import com.minprobe.app.data.remote.MonitorAuthService
import com.minprobe.app.data.remote.normalizeMonitorBaseUrl
import com.minprobe.app.data.remote.SessionManager
import com.minprobe.app.domain.model.AuthType
import com.minprobe.app.domain.model.ServerInstance
import com.minprobe.app.domain.repository.ServerRepository
import com.minprobe.app.domain.repository.SessionExpiredException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ServerRepositoryImpl(
    private val dao: ServerInstanceDao,
    private val authService: MonitorAuthService,
    private val sessionManager: SessionManager,
    private val cryptoManager: CryptoManager
) : ServerRepository {

    companion object {
        private const val TAG = "ServerRepo"
    }

    // ─── CRUD ───

    override suspend fun getAll(): List<ServerInstance> =
        dao.getAll().map { it.toDomain(cryptoManager) }

    override fun getAllFlow(): Flow<List<ServerInstance>> =
        dao.getAllFlow().map { list -> list.map { it.toDomain(cryptoManager) } }

    override suspend fun getActive(): ServerInstance? =
        dao.getActive()?.toDomain(cryptoManager)

    override fun getActiveFlow(): Flow<ServerInstance?> =
        dao.getActiveFlow().map { it?.toDomain(cryptoManager) }

    override suspend fun getById(id: Long): ServerInstance? =
        dao.getById(id)?.toDomain(cryptoManager)

    override suspend fun add(instance: ServerInstance): Long =
        dao.insert(instance.toEntity(cryptoManager))

    override suspend fun update(instance: ServerInstance) {
        val previous = dao.getById(instance.id)?.toDomain(cryptoManager)
        val normalized = normalizeMonitorBaseUrl(instance.baseUrl)
        val credentialsChanged = previous == null ||
            previous.baseUrl != normalized ||
            previous.password != instance.password ||
            previous.authType != instance.authType
        val merged = instance.copy(
            baseUrl = normalized,
            sessionCookie = if (credentialsChanged) null else
                (instance.sessionCookie ?: previous?.sessionCookie)
        )
        if (credentialsChanged && sessionManager.getServerId() == instance.id) {
            sessionManager.clearSession()
        }
        dao.update(merged.toEntity(cryptoManager))
    }

    override suspend fun remove(id: Long) {
        val entity = dao.getById(id) ?: return
        val instance = entity.toDomain(cryptoManager)
        // 先通知服务端销毁 session，再删本地
        runCatching { authService.logout(instance.baseUrl, instance.sessionCookie) }
        if (sessionManager.getServerId() == id) {
            sessionManager.clearSession()
        }
        dao.delete(entity)
    }

    override suspend fun setActive(id: Long) {
        dao.deactivateAll()
        dao.activateById(id)
    }

    // ─── 认证 ───

    override suspend fun testConnection(baseUrl: String, password: String): String {
        val normalized = normalizeMonitorBaseUrl(baseUrl)
        val cookie = authService.login(normalized, password)
        return try {
            val me = authService.me(normalized, cookie)
            if (!me.authed) throw AuthException(401, "登录后会话校验失败")
            me.site_name.ifBlank { normalized }
        } finally {
            // 测试用的 session 用完即销毁，不残留
            runCatching { authService.logout(normalized, cookie) }
        }
    }

    override suspend fun testConnectionAsGuest(baseUrl: String): String {
        val normalized = normalizeMonitorBaseUrl(baseUrl)
        val me = authService.meAsGuest(normalized)
        if (!me.publicPage) {
            throw AuthException(403, "该面板未开启公开页，访客模式不可用")
        }
        return me.site_name.ifBlank { normalized }
    }

    override suspend fun login(instance: ServerInstance): Boolean {
        val normalized = normalizeMonitorBaseUrl(instance.baseUrl)
        val target = instance.copy(baseUrl = normalized)
        if (target.authType == AuthType.GUEST) {
            // 访客模式：确认公开页可用即可
            val me = authService.meAsGuest(target.baseUrl)
            if (!me.publicPage) throw AuthException(403, "该面板未开启公开页")
            sessionManager.setSession(target.id, target.baseUrl, "", AuthType.GUEST)
            dao.updateMeta(target.id, me.site_name, me.history_days)
            return true
        }
        val cookie = authService.login(target.baseUrl, target.password)
        val me = authService.me(target.baseUrl, cookie)
        if (!me.authed) throw AuthException(401, "登录后会话校验失败")
        dao.updateSessionCookie(target.id, cryptoManager.encrypt(cookie))
        dao.updateMeta(target.id, me.site_name, me.history_days)
        sessionManager.setSession(target.id, target.baseUrl, cookie, AuthType.PASSWORD)
        Log.d(TAG, "Login ok for server ${instance.id}")
        return true
    }

    override suspend fun ensureSessionCookie(instance: ServerInstance): String {
        val active = sessionManager.activeSession.value
        val normalized = normalizeMonitorBaseUrl(instance.baseUrl)
        if (active != null && active.serverId == instance.id && active.baseUrl == normalized) {
            return active.sessionCookie
        }
        val target = instance.copy(baseUrl = normalized)
        if (target.authType == AuthType.GUEST) {
            sessionManager.setSession(target.id, target.baseUrl, "", AuthType.GUEST)
            return ""
        }
        // 1. 尝试恢复已持久化的 session
        val persistedCookie = target.sessionCookie
        if (!persistedCookie.isNullOrBlank()) {
            runCatching {
                val me = authService.me(target.baseUrl, persistedCookie)
                if (me.authed) {
                    dao.updateMeta(instance.id, me.site_name, me.history_days)
                    sessionManager.setSession(
                        target.id, target.baseUrl, persistedCookie, AuthType.PASSWORD
                    )
                    Log.d(TAG, "Session restored for server ${instance.id}")
                    return persistedCookie
                }
            }.onFailure {
                Log.w(TAG, "Session restore failed: ${it.message}")
            }
        }
        // 2. 用保存的密码重新登录
        if (target.password.isBlank()) {
            throw SessionExpiredException("会话失效且未保存密码，请重新登录")
        }
        login(target)
        return sessionManager.getSessionCookie()
            ?: throw SessionExpiredException("登录后未获得会话")
    }

    override suspend fun restoreSession(instance: ServerInstance): Boolean {
        if (instance.authType == AuthType.GUEST) {
            return runCatching {
                val me = authService.meAsGuest(instance.baseUrl)
                me.publicPage
            }.getOrDefault(false)
        }
        val cookie = instance.sessionCookie ?: return false
        return runCatching {
            val me = authService.me(instance.baseUrl, cookie)
            if (me.authed) {
                dao.updateMeta(instance.id, me.site_name, me.history_days)
                sessionManager.setSession(
                    instance.id, instance.baseUrl, cookie, AuthType.PASSWORD
                )
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    override suspend fun logout(instance: ServerInstance) {
        runCatching { authService.logout(instance.baseUrl, instance.sessionCookie) }
        dao.updateSessionCookie(instance.id, null)
        if (sessionManager.getServerId() == instance.id) {
            sessionManager.clearSession()
        }
    }

    override suspend fun updatePassword(id: Long, password: String) {
        val entity = dao.getById(id) ?: return
        dao.update(entity.copy(encryptedPassword = cryptoManager.encrypt(password)))
    }

    // ─── 映射 ───

    private fun ServerInstanceEntity.toDomain(crypto: CryptoManager): ServerInstance {
        return ServerInstance(
            id = id,
            name = name,
            baseUrl = baseUrl,
            password = runCatching { crypto.decrypt(encryptedPassword) }.getOrDefault(""),
            sessionCookie = encryptedSessionCookie?.let {
                runCatching { crypto.decrypt(it) }.getOrNull()
            },
            authType = runCatching { AuthType.valueOf(authType) }.getOrDefault(AuthType.PASSWORD),
            isActive = isActive,
            createdAt = createdAt,
            siteName = siteName,
            historyDays = historyDays
        )
    }

    private fun ServerInstance.toEntity(crypto: CryptoManager): ServerInstanceEntity {
        return ServerInstanceEntity(
            id = id,
            name = name,
            baseUrl = baseUrl.trim().trimEnd('/'),
            encryptedPassword = crypto.encrypt(password),
            encryptedSessionCookie = sessionCookie?.takeIf { it.isNotBlank() }?.let {
                crypto.encrypt(it)
            },
            authType = authType.name,
            isActive = isActive,
            createdAt = createdAt,
            siteName = siteName,
            historyDays = historyDays
        )
    }
}
