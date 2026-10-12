package com.minprobe.app.ui.screen.settings

import android.content.Context
import android.os.Build
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.minprobe.app.R
import com.minprobe.app.data.remote.UpdateCheckService
import com.minprobe.app.data.remote.UpdateInfo
import com.minprobe.app.data.remote.ApkUpdateInstaller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AboutViewModel(
    private val updateCheckService: UpdateCheckService,
    private val context: Context
) : ScreenModel {

    private val installer = ApkUpdateInstaller(context)

    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    private val _updateMessage = MutableStateFlow<String?>(null)
    val updateMessage: StateFlow<String?> = _updateMessage.asStateFlow()

    val latestUpdate: StateFlow<UpdateInfo?> = updateCheckService.latestUpdate

    fun checkUpdate() {
        if (_checking.value) return
        _checking.value = true
        _updateMessage.value = null
        screenModelScope.launch {
            try {
                val versionCode = currentVersionCode()
                val info = updateCheckService.checkForUpdate(versionCode)
                _updateMessage.value = if (info != null) {
                    context.getString(R.string.settings_update_available, info.versionName)
                } else {
                    context.getString(R.string.settings_update_latest)
                }
            } catch (e: Exception) {
                _updateMessage.value = context.getString(R.string.settings_update_failed)
            } finally {
                _checking.value = false
            }
        }
    }

    fun downloadUpdate(info: UpdateInfo): Long = installer.enqueue(info)

    fun appVersionName(): String {
        return runCatching {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, 0)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            info.versionName ?: "1.0.0"
        }.getOrDefault("1.0.0")
    }

    private fun currentVersionCode(): Int {
        return runCatching {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0).versionCode
            }
        }.getOrDefault(0)
    }
}
