package com.minprobe.app.ui.screen.server

import android.content.Context
import cafe.adriel.voyager.core.model.screenModelScope
import com.minprobe.app.R
import com.minprobe.app.domain.model.AuthType
import com.minprobe.app.domain.repository.ServerRepository
import com.minprobe.app.mvi.MviViewModel
import com.minprobe.app.ui.screen.server.ServerContract.Effect
import com.minprobe.app.ui.screen.server.ServerContract.Event
import kotlinx.coroutines.launch

/** 会话过期后的重新登录：只改密码，不动其他配置 */
class ServerReLoginViewModel(
    private val serverRepository: ServerRepository,
    private val context: Context
) : MviViewModel<ReLoginState, ReLoginEvent, ReLoginEffect>(ReLoginState()) {

    override fun onEvent(event: ReLoginEvent) {
        when (event) {
            is ReLoginEvent.PasswordChanged -> setState { copy(password = event.password) }
            is ReLoginEvent.Submit -> submit()
        }
    }

    fun loadActiveServer() {
        screenModelScope.launch {
            val server = serverRepository.getActive()
            if (server == null) {
                sendEffect(ReLoginEffect.ShowToast(context.getString(R.string.error_no_server_selected)))
                sendEffect(ReLoginEffect.NavigateBack)
                return@launch
            }
            if (server.authType == AuthType.GUEST) {
                // 访客模式无需密码，直接重试恢复
                sendEffect(ReLoginEffect.NavigateBack)
                return@launch
            }
            setState { copy(serverName = server.name, serverId = server.id) }
        }
    }

    private fun submit() {
        val state = currentState
        if (state.isLoading || state.serverId == null) return
        if (state.password.isBlank()) {
            sendEffect(ReLoginEffect.ShowToast(context.getString(R.string.server_need_password)))
            return
        }
        setState { copy(isLoading = true, error = null) }
        screenModelScope.launch {
            try {
                val server = serverRepository.getById(state.serverId)
                    ?: throw IllegalStateException("面板不存在")
                // 先用新密码登录验证
                serverRepository.testConnection(server.baseUrl, state.password)
                serverRepository.updatePassword(server.id, state.password)
                // 清掉旧 session，下次 ensureSessionCookie 会用新密码重登
                serverRepository.logout(server)
                val refreshed = serverRepository.getById(server.id)
                if (refreshed != null) {
                    serverRepository.login(refreshed)
                }
                setState { copy(isLoading = false) }
                sendEffect(ReLoginEffect.ShowToast(context.getString(R.string.server_saved)))
                sendEffect(ReLoginEffect.NavigateBack)
            } catch (e: Exception) {
                setState {
                    copy(isLoading = false, error = e.message ?: "登录失败")
                }
            }
        }
    }
}

data class ReLoginState(
    val serverId: Long? = null,
    val serverName: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) : com.minprobe.app.mvi.UiState

sealed interface ReLoginEvent : com.minprobe.app.mvi.UiEvent {
    data class PasswordChanged(val password: String) : ReLoginEvent
    data object Submit : ReLoginEvent
}

sealed interface ReLoginEffect : com.minprobe.app.mvi.UiEffect {
    data class ShowToast(val message: String) : ReLoginEffect
    data object NavigateBack : ReLoginEffect
}
