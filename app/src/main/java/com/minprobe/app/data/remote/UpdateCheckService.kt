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
 * 应用更新检查。
 *
 * 从 update.json 读取最新版本信息。注意：仅做版本提示，
 * 下载通过系统浏览器打开 release 页面完成，不在 App 内静默下载安装。
 *
 * 发布前请将 UPDATE_URL 替换为你自己的仓库地址。
 */
class UpdateCheckService(private val httpClient: HttpClient) {

    companion object {
        private const val TAG = "UpdateCheck"
        // TODO: fork 后替换为你自己的仓库地址
        private const val UPDATE_URL =
            "https://raw.githubusercontent.com/minprobe/minimal-probe-android/main/update.json"
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _latestUpdate = MutableStateFlow<UpdateInfo?>(null)
    val latestUpdate: StateFlow<UpdateInfo?> = _latestUpdate.asStateFlow()

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? {
        return runCatching {
            val response = httpClient.get(UPDATE_URL)
            if (!response.status.isSuccess()) return null
            val info = json.decodeFromString(UpdateInfo.serializer(), response.bodyAsText())
            if (info.versionCode > currentVersionCode) {
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
