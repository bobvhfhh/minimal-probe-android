package com.minprobe.app.data.repository

import com.minprobe.app.data.remote.MonitorNodeService
import com.minprobe.app.data.remote.MonitorWebSocket
import com.minprobe.app.domain.model.AuthType
import com.minprobe.app.domain.model.MetricsHistory
import com.minprobe.app.domain.model.Node
import com.minprobe.app.domain.repository.NodeRepository
import kotlinx.coroutines.flow.Flow

class NodeRepositoryImpl(
    private val nodeService: MonitorNodeService,
    private val webSocket: MonitorWebSocket
) : NodeRepository {

    override suspend fun getNodes(
        baseUrl: String,
        sessionCookie: String?,
        authType: AuthType
    ): List<Node> = nodeService.getNodes(baseUrl, sessionCookie, authType)

    override fun observeNodes(
        baseUrl: String,
        sessionCookie: String?,
        authType: AuthType
    ): Flow<List<Node>> = webSocket.observeNodes(baseUrl, sessionCookie, authType)

    override suspend fun getMetricsHistory(
        baseUrl: String,
        sessionCookie: String?,
        authType: AuthType,
        nodeId: Long,
        hours: Int
    ): MetricsHistory {
        // 先拿节点以获得内存/磁盘总量，用于换算百分比
        val node = runCatching {
            getNodes(baseUrl, sessionCookie, authType).firstOrNull { it.id == nodeId }
        }.getOrNull()
        return nodeService.getMetricsHistory(baseUrl, sessionCookie, authType, nodeId, hours, node)
    }
}
