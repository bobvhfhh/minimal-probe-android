package com.minprobe.app.data.remote

import android.util.Log
import com.minprobe.app.data.remote.dto.LoginBody
import com.minprobe.app.data.remote.dto.MeDto
import com.minprobe.app.domain.model.AuthType
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * 极简探针认证服务。
 *
 * 认证模型（见服务端 src/auth.rs）：
 * - POST /api/auth/login  body {"password": "..."}，成功返回 Set-Cookie: monitor_session=<token>
 * - GET  /api/me 返回 {"authed": true/false, "site_name": ..., "history_days": ...}
 * - POST /api/auth/logout 销毁服务端 session
 *
 * 注意服务端限流：同一 IP 5 次失败后锁定 15 分钟（429）。
 */
class MonitorAuthService(private val httpClient: HttpClient) {

    companion object {
        private const val TAG = "MonitorAuth"
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 密码登录。
     * @return 成功时返回 session cookie 值；失败抛 [AuthException]
     */
    suspend fun login(baseUrl: String, password: String): String {
        val url = baseUrl.trimEnd('/') + "/api/auth/login"
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            // 登录请求不带旧 cookie：拦截器已按 path 跳过注入
            setBody(json.encodeToString(LoginBody.serializer(), LoginBody(password)))
        }
        val status = response.status.value
        val body = response.bodyAsText()
        if (status == 429) {
            throw AuthException(status, "尝试次数过多，15 分钟后再试")
        }
        if (!response.status.isSuccess()) {
            throw AuthException(status, parseErrorMessage(body) ?: "登录失败 (HTTP $status)")
        }
        // 服务端返回 {"ok": true}，session 在 Set-Cookie 中
        val cookie = response.headers.getAll("Set-Cookie")
            ?.firstNotNullOfOrNull { parseSessionCookie(it) }
            ?: throw AuthException(status, "登录成功但未收到会话 cookie")
        Log.d(TAG, "Login succeeded, session cookie received")
        return cookie
    }

    /** 用现有 cookie 校验会话有效性，同时读取站点元信息 */
    suspend fun me(baseUrl: String, sessionCookie: String): MeDto {
        val url = baseUrl.trimEnd('/') + "/api/me"
        val response = httpClient.get(url) {
            applyAuth(sessionCookie, AuthType.PASSWORD)
        }
        if (!response.status.isSuccess()) {
            throw AuthException(response.status.value, "会话校验失败 (HTTP ${response.status.value})")
        }
        return json.decodeFromString(MeDto.serializer(), response.bodyAsText())
    }

    /** 访客模式：公开页可用时 /api/me 返回 authed=false 但 public_page=true */
    suspend fun meAsGuest(baseUrl: String): MeDto {
        val url = baseUrl.trimEnd('/') + "/api/me"
        val response = httpClient.get(url)
        if (!response.status.isSuccess()) {
            throw AuthException(response.status.value, "无法连接面板 (HTTP ${response.status.value})")
        }
        return json.decodeFromString(MeDto.serializer(), response.bodyAsText())
    }

    /** 登出：通知服务端销毁 session（失败也视为本地登出成功） */
    suspend fun logout(baseUrl: String, sessionCookie: String?) {
        runCatching {
            httpClient.post(baseUrl.trimEnd('/') + "/api/auth/logout") {
                applyAuth(sessionCookie, AuthType.PASSWORD)
            }
        }.onFailure { Log.w(TAG, "Logout request failed: ${it.message}") }
    }

    private fun parseSessionCookie(setCookie: String): String? {
        // Set-Cookie: monitor_session=<token>; HttpOnly; SameSite=Lax; Path=/; Max-Age=...
        return setCookie.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("$SESSION_COOKIE_NAME=") }
            ?.substringAfter("=")
            ?.takeIf { it.isNotBlank() }
    }

    private fun parseErrorMessage(body: String): String? {
        // 服务端错误为纯文本（如"密码错误"），成功为 {"ok":true}
        val trimmed = body.trim()
        return when {
            trimmed.isBlank() -> null
            trimmed.startsWith("{") -> runCatching {
                json.decodeFromString(
                    kotlinx.serialization.json.JsonObject.serializer(), trimmed
                )["message"]?.toString()?.trim('"')
            }.getOrNull()
            else -> trimmed.take(200)
        }
    }
}

class AuthException(val statusCode: Int, override val message: String) : Exception(message)
