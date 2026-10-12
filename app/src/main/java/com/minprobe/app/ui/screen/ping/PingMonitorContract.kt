package com.minprobe.app.ui.screen.ping

import com.minprobe.app.domain.model.Node
import com.minprobe.app.domain.model.PingTask
import com.minprobe.app.mvi.UiEffect
import com.minprobe.app.mvi.UiEvent
import com.minprobe.app.mvi.UiState

object PingMonitorContract {
    data class State(
        val tasks: List<PingTask> = emptyList(),
        val nodes: List<Node> = emptyList(),
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val error: String? = null
    ) : UiState

    sealed interface Event : UiEvent {
        data object Refresh : Event
        data class Save(val task: PingTask) : Event
        data class Delete(val taskId: Long) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data object NavigateToRelogin : Effect
    }
}
