package com.minprobe.app.ui.screen.nodedetail

import com.minprobe.app.domain.model.MetricsHistory
import com.minprobe.app.domain.model.Node
import com.minprobe.app.domain.model.PingHistory
import com.minprobe.app.domain.model.PingTask
import com.minprobe.app.mvi.UiEffect
import com.minprobe.app.mvi.UiEvent
import com.minprobe.app.mvi.UiState

object NodeDetailContract {

    data class State(
        val node: Node? = null,
        val isLoading: Boolean = true,
        val error: String? = null,
        val selectedHours: Int = 6,
        val availableHours: List<Int> = listOf(1, 6, 24),
        val history: MetricsHistory? = null,
        val isHistoryLoading: Boolean = false,
        val historyError: String? = null,
        val pingHistory: PingHistory? = null,
        val configuredPingTasks: List<PingTask> = emptyList(),
        val isPingLoading: Boolean = false,
        val pingError: String? = null
    ) : UiState

    sealed interface Event : UiEvent {
        data class HoursSelected(val hours: Int) : Event
        data object Retry : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data object NavigateToRelogin : Effect
    }
}
