package com.minprobe.app.data.remote

import android.util.Log
import com.minprobe.app.data.remote.dto.MetricsHistoryDto
import com.minprobe.app.data.remote.dto.NodeDto
import com.minprobe.app.data.remote.dto.PingHistoryDto
import com.minprobe.app.data.remote.dto.PingTaskDto
import com.minprobe.app.data.remote.dto.PingTaskSavedDto
import com.minprobe.app.data.remote.dto.PingTasksDto
import com.minprobe.app.domain.model.AuthType
import com.minprobe.app.domain.model.MetricsHistory
import com.minprobe.app.domain.model.Node
import com.minprobe.app.domain.model.PingHistory
import com.minprobe.app.domain.model.PingTask
import com.minprobe.app.domain.repository.SessionExpiredException
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * 极简探针节点数据服务。
 *
 * - GET /api/nodes → 节点数组（已登录返回全量视图；公开页访客可看公开节点）
 * - GET /api/nodes/{id}/metrics?hours=&points=&series=metrics → 历史指标
 */
class MonitorNodeService(private val httpClient: HttpClient) {

    companion object {
        private const val TAG = "MonitorNode"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val payloadParser = MonitorPayloadParser(json)

    suspend fun getNodes(
        baseUrl: String,
        sessionCookie: String?,
        authType: AuthType
    ): List<Node> {
        val url = baseUrl.trimEnd('/') + "/api/nodes"
        val response = httpClient.get(url) {
            applyAuth(sessionCookie, authType)
        }
        if (response.status.value == 401) {
            throw SessionExpiredException("需要登录后查看")
        }
        if (!response.status.isSuccess()) {
            throw NodeApiException(response.status.value, "获取节点失败 (HTTP ${response.status.value})")
        }
        val text = response.bodyAsText()
        val dtos = payloadParser.nodes(text)
        return dtos.map { it.toDomain() }
    }

    /**
     * 节点历史指标。
     * @param hours 窗口小时数（服务端按保留天数 clamp；>7天按小时聚合）
     * points 按图表宽度给预算，服务端上限 1440
     */
    suspend fun getMetricsHistory(
        baseUrl: String,
        sessionCookie: String?,
        authType: AuthType,
        nodeId: Long,
        hours: Int,
        node: Node? = null
    ): MetricsHistory {
        val url = baseUrl.trimEnd('/') + "/api/nodes/$nodeId/metrics"
        val response = httpClient.get(url) {
            applyAuth(sessionCookie, authType)
            parameter("hours", hours.coerceIn(1, 24 * 365))
            parameter("points", 240)
            parameter("series", "metrics")
        }
        if (response.status.value == 401) {
            throw SessionExpiredException("需要登录后查看")
        }
        if (!response.status.isSuccess()) {
            throw NodeApiException(response.status.value, "获取历史数据失败 (HTTP ${response.status.value})")
        }
        val dto = json.decodeFromString(
            MetricsHistoryDto.serializer(), response.bodyAsText()
        )
        val memTotal = node?.metrics?.memTotal?.takeIf { it > 0 } ?: node?.memTotal ?: 0
        val diskTotal = node?.metrics?.diskTotal?.takeIf { it > 0 } ?: node?.diskTotal ?: 0
        return dto.toDomain(memTotal, diskTotal).also {
            Log.d(TAG, "History node=$nodeId hours=$hours points=${it.points.size}")
        }
    }

    suspend fun getPingHistory(
        baseUrl: String,
        sessionCookie: String?,
        authType: AuthType,
        nodeId: Long,
        hours: Int
    ): PingHistory {
        val response = httpClient.get(baseUrl.trimEnd('/') + "/api/nodes/$nodeId/metrics") {
            applyAuth(sessionCookie, authType)
            parameter("hours", hours.coerceIn(1, 24 * 365))
            parameter("points", 60)
            parameter("series", "ping")
        }
        if (response.status.value == 401) throw SessionExpiredException("需要登录后查看")
        if (!response.status.isSuccess()) {
            throw NodeApiException(response.status.value, "获取延迟历史失败 (HTTP ${response.status.value})")
        }
        return json.decodeFromString(PingHistoryDto.serializer(), response.bodyAsText()).toDomain()
    }

    suspend fun getPingTasks(baseUrl: String, sessionCookie: String?): List<PingTask> {
        val response = httpClient.get(baseUrl.trimEnd('/') + "/api/ping-tasks") {
            applyAuth(sessionCookie, AuthType.PASSWORD)
        }
        if (response.status.value == 401) throw SessionExpiredException("需要管理员登录后管理监控")
        if (!response.status.isSuccess()) {
            throw NodeApiException(response.status.value, "获取监控任务失败 (HTTP ${response.status.value})")
        }
        return json.decodeFromString(PingTasksDto.serializer(), response.bodyAsText()).tasks.map { it.toDomain() }
    }

    suspend fun savePingTask(baseUrl: String, sessionCookie: String?, task: PingTask): Long {
        val response = httpClient.post(baseUrl.trimEnd('/') + "/api/ping-tasks") {
            applyAuth(sessionCookie, AuthType.PASSWORD)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PingTaskDto.serializer(), PingTaskDto.fromDomain(task)))
        }
        if (response.status.value == 401) throw SessionExpiredException("需要管理员登录后管理监控")
        if (!response.status.isSuccess()) {
            throw NodeApiException(response.status.value, "保存监控任务失败 (HTTP ${response.status.value})")
        }
        return json.decodeFromString(PingTaskSavedDto.serializer(), response.bodyAsText()).id
    }

    suspend fun deletePingTask(baseUrl: String, sessionCookie: String?, taskId: Long) {
        val response = httpClient.delete(baseUrl.trimEnd('/') + "/api/ping-tasks/$taskId") {
            applyAuth(sessionCookie, AuthType.PASSWORD)
        }
        if (response.status.value == 401) throw SessionExpiredException("需要管理员登录后管理监控")
        if (!response.status.isSuccess()) {
            throw NodeApiException(response.status.value, "删除监控任务失败 (HTTP ${response.status.value})")
        }
    }
}

class NodeApiException(val statusCode: Int, override val message: String) : Exception(message)
