package com.minprobe.app.ui.screen.settings

import android.content.Context
import android.net.Uri
import cafe.adriel.voyager.core.model.screenModelScope
import com.minprobe.app.R
import com.minprobe.app.data.backup.ConfigBackupManager
import com.minprobe.app.data.local.preferences.UserPreferencesRepository
import com.minprobe.app.mvi.MviViewModel
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val prefsRepo: UserPreferencesRepository,
    private val configBackupManager: ConfigBackupManager,
    private val context: Context
) : MviViewModel<SettingsContract.State, SettingsContract.Event, SettingsContract.Effect>(
    SettingsContract.State()
) {

    override fun onEvent(event: SettingsContract.Event) {
        when (event) {
            is SettingsContract.Event.SetThemeColor ->
                screenModelScope.launch { prefsRepo.setThemeColor(event.key) }
            is SettingsContract.Event.SetDarkMode ->
                screenModelScope.launch { prefsRepo.setDarkMode(event.key) }
            is SettingsContract.Event.SetLanguage ->
                screenModelScope.launch { prefsRepo.setLanguage(event.key) }
            is SettingsContract.Event.SetFontScale ->
                screenModelScope.launch { prefsRepo.setFontScale(event.scale) }
            is SettingsContract.Event.SetChartAnimation ->
                screenModelScope.launch { prefsRepo.setChartAnimation(event.enabled) }
            is SettingsContract.Event.SetBiometricEnabled ->
                screenModelScope.launch { prefsRepo.setBiometricEnabled(event.enabled) }
        }
    }

    fun exportConfig(uri: Uri) {
        if (currentState.isBackupInProgress) return
        setState { copy(isBackupInProgress = true) }
        screenModelScope.launch {
            try {
                val summary = configBackupManager.exportToUri(uri)
                sendEffect(
                    SettingsContract.Effect.ShowToast(
                        context.getString(R.string.settings_export_success, summary.serverCount)
                    )
                )
            } catch (e: Exception) {
                sendEffect(
                    SettingsContract.Effect.ShowToast(
                        context.getString(
                            R.string.settings_export_failed,
                            e.message ?: "未知错误"
                        )
                    )
                )
            } finally {
                setState { copy(isBackupInProgress = false) }
            }
        }
    }

    fun importConfig(uri: Uri) {
        if (currentState.isBackupInProgress) return
        setState { copy(isBackupInProgress = true) }
        screenModelScope.launch {
            try {
                val summary = configBackupManager.importFromUri(uri)
                sendEffect(
                    SettingsContract.Effect.ShowToast(
                        context.getString(
                            R.string.settings_import_success,
                            summary.addedServerCount + summary.updatedServerCount,
                            summary.addedServerCount,
                            summary.updatedServerCount
                        )
                    )
                )
            } catch (e: Exception) {
                sendEffect(
                    SettingsContract.Effect.ShowToast(
                        context.getString(
                            R.string.settings_import_failed,
                            e.message ?: "未知错误"
                        )
                    )
                )
            } finally {
                setState { copy(isBackupInProgress = false) }
            }
        }
    }
}
