package com.minprobe.app.data.backup

import android.content.Context
import android.net.Uri
import com.minprobe.app.domain.model.ServerInstance
import com.minprobe.app.domain.repository.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 配置备份管理器。
 *
 * 导出为用户选择的 URI（SAF），导入同理。
 * 注意：导出文件为明文 JSON（含面板密码），调用方必须先展示警告。
 */
class ConfigBackupManager(
    private val context: Context,
    private val serverRepository: ServerRepository,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true }
) {

    suspend fun exportToUri(uri: Uri): ConfigExportSummary = withContext(Dispatchers.IO) {
        val servers = serverRepository.getAll()
        val backup = ConfigBackup(
            servers = servers.map { server ->
                BackupServer(
                    name = server.name,
                    baseUrl = server.baseUrl,
                    password = server.password,
                    // session 14 天有效且可重新登录，不导出
                    sessionCookie = null,
                    authType = server.authType,
                    isActive = server.isActive,
                    createdAt = server.createdAt
                )
            }
        )
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.encodeToString(backup).toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("无法写入目标文件")
        ConfigExportSummary(backup.servers.size)
    }

    suspend fun importFromUri(uri: Uri): ConfigImportSummary = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: throw IllegalStateException("无法读取备份文件")
        val backup = json.decodeFromString(ConfigBackup.serializer(), text)
        require(backup.version <= ConfigBackup.CURRENT_VERSION) {
            "不支持的备份版本：${backup.version}"
        }

        val existing = serverRepository.getAll()
        var added = 0
        var updated = 0
        var skipped = 0
        var restoredActive = false

        for (item in backup.servers) {
            if (item.baseUrl.isBlank() || item.name.isBlank()) {
                skipped++
                continue
            }
            val match = existing.firstOrNull {
                it.baseUrl.trimEnd('/') == item.baseUrl.trimEnd('/')
            }
            if (match != null) {
                serverRepository.update(
                    match.copy(
                        name = item.name,
                        password = item.password,
                        authType = item.authType
                    )
                )
                updated++
            } else {
                val id = serverRepository.add(
                    ServerInstance(
                        name = item.name,
                        baseUrl = item.baseUrl.trimEnd('/'),
                        password = item.password,
                        authType = item.authType,
                        isActive = false
                    )
                )
                added++
                if (item.isActive) {
                    serverRepository.setActive(id)
                    restoredActive = true
                }
            }
        }
        ConfigImportSummary(added, updated, skipped, restoredActive)
    }
}
