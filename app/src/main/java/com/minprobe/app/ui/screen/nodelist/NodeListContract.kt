package com.minprobe.app.ui.screen.nodelist

import com.minprobe.app.domain.model.Node
import com.minprobe.app.mvi.UiEffect
import com.minprobe.app.mvi.UiEvent
import com.minprobe.app.mvi.UiState

object NodeListContract {

    enum class StatusFilter { ALL, ONLINE, OFFLINE }
    enum class NodeSort { DEFAULT, NAME, CPU, MEM }

    data class State(
        val serverName: String = "",
        val nodes: List<Node> = emptyList(),
        val groups: List<String> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val searchQuery: String = "",
        val selectedGroup: String? = null,
        val statusFilter: StatusFilter = StatusFilter.ALL,
        val sort: NodeSort = NodeSort.DEFAULT,
        val onlineCount: Int = 0,
        val offlineCount: Int = 0
    ) : UiState

    sealed interface Event : UiEvent {
        data class SearchQueryChanged(val query: String) : Event
        data class GroupSelected(val group: String?) : Event
        data class StatusFilterSelected(val filter: StatusFilter) : Event
        data class SortSelected(val sort: NodeSort) : Event
        data object Refresh : Event
        data object Retry : Event
        data class NodeClicked(val nodeId: Long) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data class NavigateToNodeDetail(val nodeId: Long) : Effect
        data object NavigateToRelogin : Effect
    }
}
