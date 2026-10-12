package com.minprobe.app.ui.screen.server

import com.minprobe.app.domain.model.AuthType
import com.minprobe.app.domain.model.ServerInstance
import com.minprobe.app.mvi.UiEffect
import com.minprobe.app.mvi.UiEvent
import com.minprobe.app.mvi.UiState

object ServerContract {

    data class ListState(
        val servers: List<ServerInstance> = emptyList(),
        val isLoading: Boolean = true
    ) : UiState

    data class AddState(
        val editingId: Long? = null,
        val name: String = "",
        val baseUrl: String = "https://",
        val password: String = "",
        val authType: AuthType = AuthType.PASSWORD,
        val isTesting: Boolean = false,
        val isSaving: Boolean = false,
        val testResult: String? = null,
        val testError: String? = null
    ) : UiState

    sealed interface Event : UiEvent {
        data object LoadServers : Event
        data class SelectServer(val id: Long) : Event
        data class DeleteServer(val id: Long) : Event

        data class UpdateName(val name: String) : Event
        data class UpdateBaseUrl(val url: String) : Event
        data class UpdatePassword(val password: String) : Event
        data class UpdateAuthType(val authType: AuthType) : Event
        data object TestConnection : Event
        data object SaveServer : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data object NavigateBack : Effect
        data object NavigateToNodeList : Effect
    }
}
