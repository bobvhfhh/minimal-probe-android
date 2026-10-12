package com.minprobe.app.data.remote

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

class ApkUpdateInstaller(private val context: Context) {
    fun enqueue(info: UpdateInfo): Long {
        require(UpdatePolicy.isSecureDownloadUrl(info.downloadUrl)) { "不安全的更新地址" }
        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("极简探针 ${info.versionName}")
            .setDescription(info.changelog.ifBlank { "正在下载更新" })
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, UpdatePolicy.apkFileName(info.versionName))
        return context.getSystemService(DownloadManager::class.java).enqueue(request)
    }
}
