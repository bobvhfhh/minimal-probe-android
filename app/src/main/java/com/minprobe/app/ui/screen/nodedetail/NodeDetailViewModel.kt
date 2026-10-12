package com.minprobe.app.ui.screen.nodedetail

import android.content.Context
import cafe.adriel.voyager.core.model.screenModelScope
import com.minprobe.app.R
import com.minprobe.app.domain.model.ServerInstance
import com.minprobe.app.domain.repository.NodeRepository
import com.minprobe.app.domain.repository.ServerRepository
import com.minprobe.app.domain.repository.SessionExpiredException
import com.minprobe.app.mvi.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** 节点详情：节点快照 + 历史指标图表 */
class NodeDetailViewModel(
    private val nodeId: Long,
    private val nodeRepository: NodeRepository,
    private val serverRepository: ServerRepository,
    private val context: Context
) : MviViewModel<NodeDetailContract.State, NodeDetailContract.Event, NodeDetailContract.Effect>(
    NodeDetailContract.State()
) {

    private var wsJob: Job? = null
    private var historyJob: Job? = null
    private var server: ServerInstance? = null
    private var sessionCookie: String? = null

    init {
        load()
    }

    fun onScreenStarted() {
        startLiveUpdates()
    }

    fun onScreenStopped() {
        wsJob?.cancel()
        wsJob = null
    }

    override fun onEvent(event: NodeDetailContract.Event) {
        when (event) {
            is NodeDetailContract.Event.HoursSelected -> {
                if (event.hours != currentState.selectedHours) {
                    setState { copy(selectedHours = event.hours) }
                    loadHistory()
                }
            }
            is NodeDetailContract.Event.Retry -> load()
        }
    }

    private fun load() {
        setState { copy(isLoading = true, error = null) }
        screenModelScope.launch {
            try {
                val s = serverRepository.getActive()
                    ?: throw IllegalStateException(
                        context.getString(R.string.error_no_server_selected)
                    )
                server = s
                sessionCookie = serverRepository.ensureSessionCookie(s)

                // 历史窗口上限来自服务端 retention
                val hoursOptions = listOf(1, 6, 24, 168).filter { it <= s.historyDays * 24 }
                val safeHours = if (currentState.selectedHours in hoursOptions) {
                    currentState.selectedHours
                } else {
                    6
                }
                setState {
                    copy(
                        availableHours = hoursOptions.ifEmpty { listOf(1, 6, 24) },
                        selectedHours = safeHours
                    )
                }

                val nodes = nodeRepository.getNodes(s.baseUrl, sessionCookie, s.authType)
                val node = nodes.firstOrNull { it.id == nodeId }
                    ?: throw IllegalStateException("节点不存在")
                setState { copy(node = node, isLoading = false) }

                loadHistory()
                startLiveUpdates()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /** 详情页也订阅实时推送，保持指标新鲜 */
    private fun startLiveUpdates() {
        val s = server ?: return
        if (currentState.isLoading) return
        wsJob?.cancel()
        wsJob = nodeRepository.observeNodes(s.baseUrl, sessionCookie, s.authType)
            .onEach { nodes ->
                nodes.firstOrNull { it.id == nodeId }?.let { node ->
                    setState { copy(node = node) }
                }
            }
            .catch { e ->
                if (e is SessionExpiredException) {
                    sendEffect(NodeDetailContract.Effect.NavigateToRelogin)
                }
            }
            .launchIn(screenModelScope)
    }

    private fun loadHistory() {
        val s = server ?: return
        historyJob?.cancel()
        setState { copy(isHistoryLoading = true, historyError = null) }
        historyJob = screenModelScope.launch {
            try {
                val cookie = sessionCookie ?: serverRepository.ensureSessionCookie(s)
                sessionCookie = cookie
                // 总量换算由 repository 内部处理（拉取节点快照）
                val history = nodeRepository.getMetricsHistory(
                    s.baseUrl, cookie, s.authType, nodeId,
                    currentState.selectedHours
                )
                setState { copy(history = history, isHistoryLoading = false) }
            } catch (e: Exception) {
                if (e is SessionExpiredException) {
                    sendEffect(NodeDetailContract.Effect.NavigateToRelogin)
                } else {
                    setState {
                        copy(
                            isHistoryLoading = false,
                            historyError = e.message ?: "加载失败"
                        )
                    }
                }
            }
        }
    }

    private fun handleError(e: Exception) {
        if (e is SessionExpiredException) {
            setState { copy(isLoading = false) }
            sendEffect(NodeDetailContract.Effect.NavigateToRelogin)
            return
        }
        setState {
            copy(
                isLoading = false,
                error = context.getString(R.string.node_load_failed, e.message)
            )
        }
    }
}
