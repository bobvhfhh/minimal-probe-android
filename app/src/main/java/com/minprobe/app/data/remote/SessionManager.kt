package com.minprobe.app.data.remote

import com.minprobe.app.domain.model.AuthType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Session 管理器：维护当前活跃面板的 monitor_session（内存）。
 * 持久化由 ServerRepositoryImpl 负责（加密写入 Room）。
 */
class SessionManager {

    private val _activeSession = MutableStateFlow<ActiveSession?>(null)
    val activeSession: StateFlow<ActiveSession?> = _activeSession.asStateFlow()

    fun setSession(
        serverId: Long,
        baseUrl: String,
        sessionCookie: String,
        authType: AuthType = AuthType.PASSWORD
    ) {
        _activeSession.value = ActiveSession(serverId, baseUrl, sessionCookie, authType)
    }

    fun clearSession() {
        _activeSession.value = null
    }

    fun getSessionCookie(): String? = _activeSession.value?.sessionCookie
    fun getBaseUrl(): String? = _activeSession.value?.baseUrl
    fun getServerId(): Long? = _activeSession.value?.serverId
    fun getAuthType(): AuthType? = _activeSession.value?.authType
}

data class ActiveSession(
    val serverId: Long,
    val baseUrl: String,
    val sessionCookie: String,
    val authType: AuthType = AuthType.PASSWORD
)
