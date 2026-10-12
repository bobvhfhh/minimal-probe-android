package com.minprobe.app.data.backup

import com.minprobe.app.domain.model.AuthType
import kotlinx.serialization.Serializable

/**
 * 配置备份格式。
 *
 * 安全注意：这是明文 JSON，包含面板密码。导出前 UI 层必须向用户
 * 展示警告（R.string.settings_export_plaintext_warning）。
 */
@Serializable
data class ConfigBackup(
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val servers: List<BackupServer> = emptyList()
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class BackupServer(
    val name: String,
    val baseUrl: String,
    val password: String = "",
    val sessionCookie: String? = null,
    val authType: AuthType = AuthType.PASSWORD,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class ConfigExportSummary(val serverCount: Int)

data class ConfigImportSummary(
    val addedServerCount: Int,
    val updatedServerCount: Int,
    val skippedServerCount: Int,
    val restoredActiveServer: Boolean
)
