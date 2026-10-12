package com.minprobe.app.ui.screen.server

import android.content.Context
import cafe.adriel.voyager.core.model.screenModelScope
import com.minprobe.app.R
import com.minprobe.app.data.remote.AuthException
import com.minprobe.app.data.remote.normalizeMonitorBaseUrl
import com.minprobe.app.domain.model.AuthType
import com.minprobe.app.domain.model.ServerInstance
import com.minprobe.app.domain.repository.ServerRepository
import com.minprobe.app.mvi.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** 面板列表 + 添加/编辑（单 ViewModel 承载两屏状态） */
class ServerViewModel(
    private val serverRepository: ServerRepository,
    private val context: Context
) : MviViewModel<ServerContract.ListState, ServerContract.Event, ServerContract.Effect>(
    ServerContract.ListState()
) {

    private var listJob: Job? = null

    /** 添加/编辑屏的独立状态（不进 MVI state，避免列表重组） */
    private val _addState = MutableStateFlow(ServerContract.AddState())
    val addState: StateFlow<ServerContract.AddState> = _addState.asStateFlow()

    private fun updateAddState(reduce: ServerContract.AddState.() -> ServerContract.AddState) {
        _addState.value = _addState.value.reduce()
    }

    init {
        observeServers()
    }

    private fun observeServers() {
        listJob?.cancel()
        listJob = screenModelScope.launch {
            serverRepository.getAllFlow().collectLatest { servers ->
                setState { copy(servers = servers, isLoading = false) }
            }
        }
    }

    /** 进入添加/编辑屏时初始化表单 */
    fun initAddForm(editingId: Long?) {
        updateAddState {
            if (editingId == null) {
                ServerContract.AddState()
            } else {
                val s = currentState.servers.firstOrNull { it.id == editingId }
                ServerContract.AddState(
                    editingId = editingId,
                    name = s?.name.orEmpty(),
                    baseUrl = s?.baseUrl ?: "https://",
                    password = s?.password.orEmpty(),
                    authType = s?.authType ?: AuthType.PASSWORD
                )
            }
        }
    }

    override fun onEvent(event: ServerContract.Event) {
        when (event) {
            is ServerContract.Event.LoadServers -> observeServers()
            is ServerContract.Event.SelectServer -> selectServer(event.id)
            is ServerContract.Event.DeleteServer -> deleteServer(event.id)
            is ServerContract.Event.UpdateName ->
                updateAddState { copy(name = event.name, testResult = null, testError = null) }
            is ServerContract.Event.UpdateBaseUrl ->
                updateAddState { copy(baseUrl = event.url, testResult = null, testError = null) }
            is ServerContract.Event.UpdatePassword ->
                updateAddState { copy(password = event.password, testResult = null, testError = null) }
            is ServerContract.Event.UpdateAuthType ->
                updateAddState { copy(authType = event.authType, testResult = null, testError = null) }
            is ServerContract.Event.TestConnection -> testConnection()
            is ServerContract.Event.SaveServer -> saveServer()
        }
    }

    private fun selectServer(id: Long) {
        screenModelScope.launch {
            runCatching { serverRepository.setActive(id) }
                .onSuccess { sendEffect(ServerContract.Effect.NavigateToNodeList) }
                .onFailure {
                    sendEffect(
                        ServerContract.Effect.ShowToast(it.message ?: "切换失败")
                    )
                }
        }
    }

    private fun deleteServer(id: Long) {
        screenModelScope.launch {
            runCatching { serverRepository.remove(id) }
                .onSuccess {
                    sendEffect(
                        ServerContract.Effect.ShowToast(
                            context.getString(R.string.server_deleted)
                        )
                    )
                }
                .onFailure {
                    sendEffect(ServerContract.Effect.ShowToast(it.message ?: "删除失败"))
                }
        }
    }

    private fun testConnection() {
        val state = _addState.value
        if (state.isTesting) return
        if (!validateForm(state)) return
        updateAddState { copy(isTesting = true, testResult = null, testError = null) }
        screenModelScope.launch {
            try {
                val siteName = if (state.authType == AuthType.GUEST) {
                    serverRepository.testConnectionAsGuest(state.baseUrl.trim().trimEnd('/'))
                    context.getString(R.string.server_test_guest_ok)
                } else {
                    val name = serverRepository.testConnection(
                        state.baseUrl.trim().trimEnd('/'), state.password
                    )
                    context.getString(R.string.server_test_ok, name)
                }
                updateAddState { copy(isTesting = false, testResult = siteName) }
            } catch (e: Exception) {
                val msg = when (e) {
                    is AuthException -> e.message
                    else -> e.message ?: context.getString(R.string.server_test_failed, "")
                }
                updateAddState {
                    copy(
                        isTesting = false,
                        testError = context.getString(R.string.server_test_failed, msg)
                    )
                }
            }
        }
    }

    private fun saveServer() {
        val state = _addState.value
        if (state.isSaving) return
        if (!validateForm(state)) return
        updateAddState { copy(isSaving = true) }
        screenModelScope.launch {
            try {
                val normalized = ServerInstance(
                    id = state.editingId ?: 0,
                    name = state.name.trim(),
                    baseUrl = state.baseUrl.trim().trimEnd('/'),
                    password = state.password,
                    authType = state.authType,
                    isActive = false,
                    createdAt = System.currentTimeMillis()
                )
                if (state.editingId == null) {
                    val id = serverRepository.add(normalized)
                    // 第一个面板自动设为活跃
                    if (currentState.servers.isEmpty()) {
                        serverRepository.setActive(id)
                    }
                    // 保存后立即登录一次，拿到 session
                    runCatching {
                        val saved = serverRepository.getById(id)
                        if (saved != null) serverRepository.login(saved)
                    }
                } else {
                    val old = currentState.servers.firstOrNull { it.id == state.editingId }
                    serverRepository.update(
                        normalized.copy(
                            id = state.editingId,
                            isActive = old?.isActive ?: false,
                            createdAt = old?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                }
                updateAddState { copy(isSaving = false) }
                sendEffect(
                    ServerContract.Effect.ShowToast(context.getString(R.string.server_saved))
                )
                sendEffect(ServerContract.Effect.NavigateBack)
            } catch (e: Exception) {
                updateAddState { copy(isSaving = false) }
                sendEffect(
                    ServerContract.Effect.ShowToast(e.message ?: "保存失败")
                )
            }
        }
    }

    private fun validateForm(state: ServerContract.AddState): Boolean {
        if (state.name.isBlank()) {
            sendEffect(ServerContract.Effect.ShowToast(context.getString(R.string.server_need_name)))
            return false
        }
        val normalizedUrl = runCatching { normalizeMonitorBaseUrl(state.baseUrl) }.getOrNull()
        if (normalizedUrl == null) {
            sendEffect(ServerContract.Effect.ShowToast(context.getString(R.string.server_need_url)))
            return false
        }
        if (state.authType == AuthType.PASSWORD && state.password.isBlank()) {
            sendEffect(
                ServerContract.Effect.ShowToast(context.getString(R.string.server_need_password))
            )
            return false
        }
        updateAddState { copy(baseUrl = normalizedUrl) }
        return true
    }
}
