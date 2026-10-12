package com.minprobe.app.domain.repository

import com.minprobe.app.domain.model.AuthType
import com.minprobe.app.domain.model.MetricsHistory
import com.minprobe.app.domain.model.Node
import com.minprobe.app.domain.model.ServerInstance
import kotlinx.coroutines.flow.Flow

/** 会话失效，需要重新登录 */
class SessionExpiredException(message: String? = null) : Exception(message)

/** 面板实例仓库 */
interface ServerRepository {
    suspend fun getAll(): List<ServerInstance>
    fun getAllFlow(): Flow<List<ServerInstance>>
    suspend fun getActive(): ServerInstance?
    fun getActiveFlow(): Flow<ServerInstance?>
    suspend fun getById(id: Long): ServerInstance?
    suspend fun add(instance: ServerInstance): Long
    suspend fun update(instance: ServerInstance)
    suspend fun remove(id: Long)
    suspend fun setActive(id: Long)

    /** 用密码测试连接，成功返回站点名 */
    suspend fun testConnection(baseUrl: String, password: String): String

    /** 访客模式测试连接（要求面板开启公开页） */
    suspend fun testConnectionAsGuest(baseUrl: String): String

    /** 登录并持久化 session cookie，成功返回 true */
    suspend fun login(instance: ServerInstance): Boolean

    /** 确保拿到可用的 session cookie（自动恢复/重新登录） */
    suspend fun ensureSessionCookie(instance: ServerInstance): String

    /** 尝试恢复已持久化的 session（/api/me 校验） */
    suspend fun restoreSession(instance: ServerInstance): Boolean

    /** 登出：通知服务端并清除本地 session */
    suspend fun logout(instance: ServerInstance)

    /** 更新登录密码（加密存储） */
    suspend fun updatePassword(id: Long, password: String)
}

/** 节点仓库 */
interface NodeRepository {
    /** 一次性拉取节点列表（HTTP） */
    suspend fun getNodes(baseUrl: String, sessionCookie: String?, authType: AuthType): List<Node>

    /** WebSocket 实时推送节点列表（hub 每 2 秒推全量帧） */
    fun observeNodes(baseUrl: String, sessionCookie: String?, authType: AuthType): Flow<List<Node>>

    /** 节点历史指标 */
    suspend fun getMetricsHistory(
        baseUrl: String,
        sessionCookie: String?,
        authType: AuthType,
        nodeId: Long,
        hours: Int
    ): MetricsHistory
}
