package com.minprobe.app.data.remote

import android.util.Log
import com.minprobe.app.domain.model.AuthType
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Session Cookie 注入拦截器（已修复跨服务器凭证泄露问题）。
 *
 * 注入必须同时满足三个条件：
 * 1. 存在活跃 session；
 * 2. 请求 host 与 session 所属面板的 host 一致（scheme+host+port 均取自 baseUrl）；
 * 3. 请求不是登录请求（/api/auth/login 走 body 密码，不需要也不应带旧 cookie）。
 *
 * 条件 2 修复了旧实现的问题：测试新面板连接时，会把旧面板的
 * monitor_session 发给新面板。
 */
class SessionCookieInterceptor(private val sessionManager: SessionManager) : Interceptor {

    companion object {
        private const val TAG = "SessionCookie"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val session = sessionManager.activeSession.value
        val originalRequest = chain.request()

        if (session == null || session.authType == AuthType.GUEST) {
            return chain.proceed(originalRequest)
        }

        val path = originalRequest.url.encodedPath
        if (path.startsWith("/api/auth/login")) {
            return chain.proceed(originalRequest)
        }

        val sessionUrl = session.baseUrl.trimEnd('/').toHttpUrlOrNull()
            ?: return chain.proceed(originalRequest)

        val sameOrigin = originalRequest.url.scheme.equals(sessionUrl.scheme, ignoreCase = true) &&
            originalRequest.url.host.equals(sessionUrl.host, ignoreCase = true) &&
            originalRequest.url.port == sessionUrl.port
        if (!sameOrigin) {
            Log.d(TAG, "Skipping cookie injection: origin mismatch " +
                "(${originalRequest.url} != $sessionUrl)")
            return chain.proceed(originalRequest)
        }

        val newRequest = originalRequest.newBuilder()
            .header("Cookie", buildSessionCookieHeader(session.sessionCookie))
            .build()
        return chain.proceed(newRequest)
    }
}
