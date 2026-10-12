package com.minprobe.app.data.remote

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 从仓库中的 update.json 检查可用更新。APK 下载及安装由 Android 系统安装器完成。
 */
class UpdateCheckService(private val httpClient: HttpClient) {

    companion object {
        private const val TAG = "UpdateCheck"
        private const val UPDATE_URL =
            "https://raw.githubusercontent.com/bobvhfhh/minimal-probe-android/master/update.json"
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _latestUpdate = MutableStateFlow<UpdateInfo?>(null)
    val latestUpdate: StateFlow<UpdateInfo?> = _latestUpdate.asStateFlow()

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? {
        return runCatching {
            val response = httpClient.get(UPDATE_URL)
            if (!response.status.isSuccess()) return null
            val info = json.decodeFromString(UpdateInfo.serializer(), response.bodyAsText())
            if (UpdatePolicy.isNewer(info.versionCode, currentVersionCode) &&
                UpdatePolicy.isSecureDownloadUrl(info.downloadUrl)
            ) {
                _latestUpdate.value = info
                info
            } else {
                _latestUpdate.value = null
                null
            }
        }.onFailure {
            Log.w(TAG, "Update check failed: ${it.message}")
        }.getOrNull()
    }

    suspend fun checkForUpdateSilent(currentVersionCode: Int) {
        runCatching { checkForUpdate(currentVersionCode) }
    }
}

@Serializable
data class UpdateInfo(
    val versionCode: Int = 0,
    val versionName: String = "",
    val changelog: String = "",
    val downloadUrl: String = ""
)
