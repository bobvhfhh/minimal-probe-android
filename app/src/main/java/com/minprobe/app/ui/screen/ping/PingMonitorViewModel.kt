package com.minprobe.app.ui.screen.ping

import android.content.Context
import cafe.adriel.voyager.core.model.screenModelScope
import com.minprobe.app.R
import com.minprobe.app.domain.model.AuthType
import com.minprobe.app.domain.model.PingTask
import com.minprobe.app.domain.repository.NodeRepository
import com.minprobe.app.domain.repository.ServerRepository
import com.minprobe.app.domain.repository.SessionExpiredException
import com.minprobe.app.mvi.MviViewModel
import kotlinx.coroutines.launch

class PingMonitorViewModel(
    private val nodeRepository: NodeRepository,
    private val serverRepository: ServerRepository,
    private val context: Context
) : MviViewModel<PingMonitorContract.State, PingMonitorContract.Event, PingMonitorContract.Effect>(
    PingMonitorContract.State()
) {
    private var baseUrl = ""
    private var cookie: String? = null

    init { refresh() }

    override fun onEvent(event: PingMonitorContract.Event) = when (event) {
        PingMonitorContract.Event.Refresh -> refresh()
        is PingMonitorContract.Event.Save -> save(event.task)
        is PingMonitorContract.Event.Delete -> delete(event.taskId)
    }

    private fun refresh() {
        setState { copy(isLoading = true, error = null) }
        screenModelScope.launch {
            try {
                val server = serverRepository.getActive()
                    ?: throw IllegalStateException(context.getString(R.string.error_no_server_selected))
                if (server.authType != AuthType.PASSWORD) {
                    throw IllegalStateException("访客模式只能查看延迟，不能管理 TCP 监控")
                }
                baseUrl = server.baseUrl
                cookie = serverRepository.ensureSessionCookie(server)
                val nodes = nodeRepository.getNodes(baseUrl, cookie, server.authType)
                val tasks = nodeRepository.getPingTasks(baseUrl, cookie)
                setState { copy(nodes = nodes, tasks = tasks, isLoading = false) }
            } catch (e: Exception) { handle(e) }
        }
    }

    private fun save(task: PingTask) {
        val validation = validatePingTask(task.name, task.target, task.intervalSeconds)
        if (validation != null) { sendEffect(PingMonitorContract.Effect.ShowToast(validation)); return }
        if (task.nodeIds.isEmpty()) { sendEffect(PingMonitorContract.Effect.ShowToast("请至少选择一个节点")); return }
        if (baseUrl.isBlank()) return refresh()
        setState { copy(isSaving = true) }
        screenModelScope.launch {
            try {
                nodeRepository.savePingTask(baseUrl, cookie, task)
                setState { copy(isSaving = false) }
                sendEffect(PingMonitorContract.Effect.ShowToast("TCP 监控已保存，节点会立即收到新任务"))
                refresh()
            } catch (e: Exception) { setState { copy(isSaving = false) }; handle(e) }
        }
    }

    private fun delete(taskId: Long) {
        if (baseUrl.isBlank()) return
        screenModelScope.launch {
            try {
                nodeRepository.deletePingTask(baseUrl, cookie, taskId)
                sendEffect(PingMonitorContract.Effect.ShowToast("监控已删除"))
                refresh()
            } catch (e: Exception) { handle(e) }
        }
    }

    private fun handle(e: Exception) {
        if (e is SessionExpiredException) {
            setState { copy(isLoading = false) }
            sendEffect(PingMonitorContract.Effect.NavigateToRelogin)
        } else {
            setState { copy(isLoading = false, error = e.message ?: "加载失败") }
        }
    }
}
