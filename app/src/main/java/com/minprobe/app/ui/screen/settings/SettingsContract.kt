package com.minprobe.app.ui.screen.settings

import com.minprobe.app.mvi.UiEffect
import com.minprobe.app.mvi.UiEvent
import com.minprobe.app.mvi.UiState

object SettingsContract {

    data class State(
        val isBackupInProgress: Boolean = false
    ) : UiState

    sealed interface Event : UiEvent {
        data class SetThemeColor(val key: String) : Event
        data class SetDarkMode(val key: String) : Event
        data class SetLanguage(val key: String) : Event
        data class SetFontScale(val scale: Float) : Event
        data class SetChartAnimation(val enabled: Boolean) : Event
        data class SetBiometricEnabled(val enabled: Boolean) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
    }
}
