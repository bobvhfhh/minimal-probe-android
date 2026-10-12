package com.minprobe.app.data.remote

import android.util.Log
import com.minprobe.app.domain.model.AuthType
import com.minprobe.app.domain.model.Node
import com.minprobe.app.domain.repository.SessionExpiredException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.websocket.Frame
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlin.math.min
import kotlin.random.Random

/**
 * 极简探针实时推送（GET /api/ws）。
 *
 * 服务端行为（见 src/api.rs stream_live）：
 * - 已登录客户端每 2 秒收到一帧：完整节点数组的 JSON 文本帧
 * - 10 秒无帧视为断流（服务端要求客户端重连）
 * - 会话失效时服务端直接关闭连接（客户端应重新走 /api/me 校验）
 */
class MonitorWebSocket(private val httpClient: HttpClient) {

    companion object {
        private const val TAG = "MonitorWs"
        /** 无帧超时：服务端 2s/帧，10s 无帧即认为断流 */
        private const val FRAME_TIMEOUT_MS = 12_000L
        private const val INITIAL_RECONNECT_MS = 2_000L
        private const val MAX_RECONNECT_MS = 30_000L
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val payloadParser = MonitorPayloadParser(json)

    fun observeNodes(
        baseUrl: String,
        sessionCookie: String?,
        authType: AuthType
    ): Flow<List<Node>> = channelFlow {
        val endpoint = buildWsEndpoint(baseUrl)
        var reconnectDelay = INITIAL_RECONNECT_MS

        while (isActive) {
            try {
                Log.d(TAG, "Connecting to $endpoint")
                val session = httpClient.webSocketSession(
                    method = HttpMethod.Get,
                    host = endpoint.host,
                    port = endpoint.port,
                    path = endpoint.path
                ) {
                    url.protocol = endpoint.protocol
                    if (authType != AuthType.GUEST && !sessionCookie.isNullOrBlank()) {
                        header("Cookie", buildSessionCookieHeader(sessionCookie))
                    }
                }

                // 连接成功，重置退避
                reconnectDelay = INITIAL_RECONNECT_MS

                while (isActive) {
                    val frame = withTimeoutOrNull(FRAME_TIMEOUT_MS) {
                        session.incoming.receive()
                    }
                    if (frame == null) {
                        Log.w(TAG, "No frame for ${FRAME_TIMEOUT_MS}ms, reconnecting")
                        break
                    }
                    when (frame) {
                        is Frame.Text -> {
                            val nodes = runCatching {
                                payloadParser.nodes(frame.readText()).map { it.toDomain() }
                            }.getOrElse { e ->
                                Log.w(TAG, "Frame parse error: ${e.message}")
                                null
                            }
                            if (nodes != null) {
                                trySend(nodes)
                            }
                        }
                        is Frame.Close -> {
                            Log.w(TAG, "Server closed WS: ${frame.readReason()}")
                            // 服务端在会话失效时会关闭连接
                            throw SessionExpiredException("连接被服务端关闭，会话可能已失效")
                        }
                        else -> Unit // ping/pong/binary 忽略
                    }
                }
                // Ktor 3.x 的 WebSocketSession 没有 close()，cancel scope 即可关闭连接
                runCatching { session.cancel("done") }
            } catch (e: SessionExpiredException) {
                throw e
            } catch (e: Exception) {
                if (!isActive) break
                Log.w(TAG, "WS error: ${e.message}, retry in ${reconnectDelay}ms")
            }

            // 指数退避 + 抖动后重连
            val jitter = Random.nextLong(0, 1_000)
            delay(min(reconnectDelay + jitter, MAX_RECONNECT_MS))
            reconnectDelay = min(reconnectDelay * 2, MAX_RECONNECT_MS)
        }
    }

    private data class WsEndpoint(
        val protocol: URLProtocol,
        val host: String,
        val port: Int,
        val path: String
    )

    private fun buildWsEndpoint(baseUrl: String): WsEndpoint {
        val trimmed = baseUrl.trim().trimEnd('/')
        val parsed = io.ktor.http.Url(trimmed)
        val protocol = when (parsed.protocol.name.lowercase()) {
            "http" -> URLProtocol.WS
            "https" -> URLProtocol.WSS
            else -> throw IllegalArgumentException("不支持的协议: ${parsed.protocol.name}")
        }
        val prefix = parsed.encodedPath.trimEnd('/')
        val path = "$prefix/api/ws".replace("//", "/")
        val port = if (parsed.port == 0) protocol.defaultPort else parsed.port
        return WsEndpoint(protocol, parsed.host, port, path)
    }
}
