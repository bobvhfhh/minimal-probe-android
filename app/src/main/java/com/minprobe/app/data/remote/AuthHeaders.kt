package com.minprobe.app.data.remote

import com.minprobe.app.domain.model.AuthType
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header

/** 极简探针的会话 cookie 名（见服务端 src/auth.rs COOKIE） */
internal const val SESSION_COOKIE_NAME = "monitor_session"

internal fun buildSessionCookieHeader(sessionCookie: String): String =
    "$SESSION_COOKIE_NAME=$sessionCookie"

internal fun HttpRequestBuilder.applyAuth(sessionCookie: String?, authType: AuthType) {
    if (authType == AuthType.GUEST) return
    if (sessionCookie.isNullOrBlank()) return
    header("Cookie", buildSessionCookieHeader(sessionCookie))
}
