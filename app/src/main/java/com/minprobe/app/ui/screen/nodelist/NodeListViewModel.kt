package com.minprobe.app.ui.screen.nodelist

import android.content.Context
import cafe.adriel.voyager.core.model.screenModelScope
import com.minprobe.app.R
import com.minprobe.app.domain.model.AuthType
import com.minprobe.app.domain.model.Node
import com.minprobe.app.domain.model.PingHistory
import com.minprobe.app.domain.model.ServerInstance
import com.minprobe.app.domain.repository.NodeRepository
import com.minprobe.app.domain.repository.ServerRepository
import com.minprobe.app.domain.repository.SessionExpiredException
import com.minprobe.app.mvi.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** 节点列表：HTTP 首屏 + WebSocket 实时推送（hub 每 2s 推全量帧） */
class NodeListViewModel(
    private val nodeRepository: NodeRepository,
    private val serverRepository: ServerRepository,
    private val context: Context
) : MviViewModel<NodeListContract.State, NodeListContract.Event, NodeListContract.Effect>(
    NodeListContract.State()
) {

    private var wsJob: Job? = null
    private var pingJob: Job? = null
    private var isScreenStarted = false
    private var activeServer: ServerInstance? = null
    private var sessionCookie: String? = null

    init {
        loadNodes()
    }

    fun onScreenStarted() {
        if (isScreenStarted) return
        isScreenStarted = true
        resumeStreaming()
    }

    fun onScreenStopped() {
        isScreenStarted = false
        wsJob?.cancel()
        wsJob = null
    }

    override fun onEvent(event: NodeListContract.Event) {
        when (event) {
            is NodeListContract.Event.SearchQueryChanged ->
                setState { copy(searchQuery = event.query) }
            is NodeListContract.Event.GroupSelected ->
                setState { copy(selectedGroup = event.group) }
            is NodeListContract.Event.StatusFilterSelected ->
                setState { copy(statusFilter = event.filter) }
            is NodeListContract.Event.SortSelected ->
                setState { copy(sort = event.sort) }
            is NodeListContract.Event.Refresh -> refreshNodes()
            is NodeListContract.Event.Retry -> loadNodes()
            is NodeListContract.Event.NodeClicked ->
                sendEffect(NodeListContract.Effect.NavigateToNodeDetail(event.nodeId))
        }
    }

    private fun loadNodes() {
        setState { copy(isLoading = true, error = null) }
        screenModelScope.launch {
            try {
                val server = serverRepository.getActive()
                    ?: throw IllegalStateException(
                        context.getString(R.string.error_no_server_selected)
                    )
                activeServer = server
                setState { copy(serverName = server.name.ifBlank { server.baseUrl }) }
                sessionCookie = serverRepository.ensureSessionCookie(server)

                val nodes = nodeRepository.getNodes(
                    server.baseUrl, sessionCookie, server.authType
                )
                updateNodesState(nodes)
                setState { copy(isLoading = false) }
                loadPingData(nodes)

                if (isScreenStarted) startStreaming()
            } catch (e: Exception) {
                handleError(e, initial = true)
            }
        }
    }

    private fun refreshNodes() {
        val server = activeServer ?: return loadNodes()
        setState { copy(isRefreshing = true) }
        screenModelScope.launch {
            try {
                sessionCookie = serverRepository.ensureSessionCookie(server)
                val nodes = nodeRepository.getNodes(
                    server.baseUrl, sessionCookie, server.authType
                )
                updateNodesState(nodes)
                setState { copy(isRefreshing = false, error = null) }
                loadPingData(nodes)
            } catch (e: Exception) {
                setState { copy(isRefreshing = false) }
                handleError(e, initial = false)
            }
        }
    }

    /** 切回前台时恢复推送 */
    private fun resumeStreaming() {
        val server = activeServer ?: return
        if (currentState.isLoading || wsJob != null) return
        screenModelScope.launch {
            try {
                sessionCookie = serverRepository.ensureSessionCookie(server)
                startStreaming()
            } catch (e: Exception) {
                handleError(e, initial = false)
            }
        }
    }

    private fun startStreaming() {
        val server = activeServer ?: return
        val cookie = sessionCookie
        wsJob?.cancel()
        wsJob = nodeRepository.observeNodes(server.baseUrl, cookie, server.authType)
            .onEach { nodes -> updateNodesState(nodes) }
            .catch { e -> handleError(e as? Exception ?: Exception(e), initial = false) }
            .launchIn(screenModelScope)
    }

    private fun updateNodesState(nodes: List<Node>) {
        // 按 sort 字段 + 在线状态排序，保持服务端顺序语义
        val sorted = nodes.sortedWith(
            compareByDescending<Node> { it.online }.thenBy { it.sort }.thenBy { it.name }
        )
        val groups = nodes.map { it.group }.filter { it.isNotBlank() }.distinct().sorted()
        val online = nodes.count { it.online }
        setState {
            copy(
                nodes = sorted,
                groups = groups,
                onlineCount = online,
                offlineCount = nodes.size - online
            )
        }
    }

    /**
     * 列表上的延迟来自 Hub 的历史接口。一次最多 4 个请求，匹配服务端
     * HISTORY_GATE，避免 13+ 个节点同时刷新时把服务端历史查询队列打满。
     */
    private fun loadPingData(nodes: List<Node>) {
        val server = activeServer ?: return
        pingJob?.cancel()
        pingJob = screenModelScope.launch {
            val cookie = sessionCookie ?: return@launch
            val tasks = if (server.authType == AuthType.PASSWORD) {
                runCatching { nodeRepository.getPingTasks(server.baseUrl, cookie) }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
            val gate = Semaphore(4)
            val histories = coroutineScope {
                nodes.filter { it.online }.map { node ->
                    async {
                        val history = runCatching {
                            gate.withPermit {
                                nodeRepository.getPingHistory(server.baseUrl, cookie, server.authType, node.id, hours = 1)
                            }
                        }.getOrDefault(PingHistory())
                        node.id to history
                    }
                }.awaitAll().toMap()
            }
            setState { copy(pingTasks = tasks, pingHistories = histories) }
        }
    }

    private fun handleError(e: Exception, initial: Boolean) {
        if (e is SessionExpiredException ||
            (e.message?.contains("401", ignoreCase = true) == true)
        ) {
            wsJob?.cancel()
            setState { copy(isLoading = false, isRefreshing = false) }
            // 密码模式：回退到重新登录；访客模式：提示
            val server = activeServer
            if (server != null && server.authType == AuthType.PASSWORD) {
                sendEffect(NodeListContract.Effect.NavigateToRelogin)
            } else {
                sendEffect(
                    NodeListContract.Effect.ShowToast(
                        e.message ?: context.getString(R.string.error_no_session)
                    )
                )
            }
            return
        }
        if (initial) {
            setState {
                copy(
                    isLoading = false,
                    error = context.getString(R.string.node_load_failed, e.message)
                )
            }
        } else {
            sendEffect(
                NodeListContract.Effect.ShowToast(
                    context.getString(R.string.node_refresh_failed, e.message)
                )
            )
        }
    }
}
