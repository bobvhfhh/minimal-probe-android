package com.minprobe.app.data.remote

import io.ktor.http.Url

internal fun normalizeMonitorBaseUrl(raw: String): String {
    val value = raw.trim().trimEnd('/')
    require(value.isNotBlank()) { "面板地址不能为空" }
    val url = Url(value)
    require(url.protocol.name.equals("http", true) || url.protocol.name.equals("https", true)) {
        "面板地址必须使用 http 或 https"
    }
    require(url.host.isNotBlank()) { "面板地址缺少主机名" }
    return value
}
