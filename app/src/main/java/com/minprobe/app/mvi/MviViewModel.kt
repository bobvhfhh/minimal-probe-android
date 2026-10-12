package com.minprobe.app.mvi

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * MVI 基础 ViewModel（基于 Voyager ScreenModel）
 *
 * @param S UI 状态，通过 StateFlow 暴露
 * @param E 用户事件
 * @param F 一次性副作用，通过 Channel 发送
 */
abstract class MviViewModel<S : UiState, E : UiEvent, F : UiEffect>(initialState: S) : ScreenModel {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    protected val currentState: S
        get() = _state.value

    private val _effect = Channel<F>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    abstract fun onEvent(event: E)

    protected fun setState(reduce: S.() -> S) {
        _state.value = currentState.reduce()
    }

    protected fun sendEffect(effect: F) {
        screenModelScope.launch { _effect.send(effect) }
    }
}
