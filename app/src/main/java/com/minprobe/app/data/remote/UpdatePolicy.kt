package com.minprobe.app.data.remote

internal object UpdatePolicy {
    fun isNewer(remoteVersionCode: Int, currentVersionCode: Int): Boolean =
        remoteVersionCode > currentVersionCode

    fun apkFileName(versionName: String): String {
        val safeVersion = versionName.replace(Regex("[^A-Za-z0-9._-]"), "-")
            .trim('-', '.', '_')
            .ifBlank { "latest" }
        return "MinimalProbe-v$safeVersion-release.apk"
    }

    fun isSecureDownloadUrl(value: String): Boolean = runCatching {
        java.net.URI(value).let { it.scheme.equals("https", ignoreCase = true) && !it.host.isNullOrBlank() }
    }.getOrDefault(false)
}
