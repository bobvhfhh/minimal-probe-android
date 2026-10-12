package com.minprobe.app.data.remote.dto

import com.minprobe.app.domain.model.MetricPoint
import com.minprobe.app.domain.model.MetricsHistory
import com.minprobe.app.domain.model.Node
import com.minprobe.app.domain.model.NodeMetrics
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 实时指标（/api/nodes 行内 metrics 对象） */
@Serializable
data class MetricsDto(
    val uptime: Long = 0,
    val cpu: Double = 0.0,
    val load: List<Double> = emptyList(),
    val mem_total: Long = 0,
    val mem_used: Long = 0,
    val swap_total: Long = 0,
    val swap_used: Long = 0,
    val disk_total: Long = 0,
    val disk_used: Long = 0,
    val net_rx: Long = 0,
    val net_tx: Long = 0,
    val tcp: Long = 0,
    val udp: Long = 0,
    val procs: Long = 0
) {
    fun toDomain() = NodeMetrics(
        uptime = uptime,
        cpu = cpu,
        load1 = load.getOrElse(0) { 0.0 },
        load5 = load.getOrElse(1) { 0.0 },
        load15 = load.getOrElse(2) { 0.0 },
        memTotal = mem_total,
        memUsed = mem_used,
        swapTotal = swap_total,
        swapUsed = swap_used,
        diskTotal = disk_total,
        diskUsed = disk_used,
        netRx = net_rx,
        netTx = net_tx,
        tcp = tcp,
        udp = udp,
        procs = procs
    )
}

/** 节点视图（GET /api/nodes 数组元素） */
@Serializable
data class NodeDto(
    val id: Long,
    val name: String = "",
    val country: String = "",
    val group: String = "",
    val public_remark: String = "",
    val sort: Int = 0,
    val public: Boolean = false,
    val online: Boolean = false,
    val last_seen: Long = 0,
    val last_seen_ago: Long? = null,
    val metrics: MetricsDto? = null,
    val os: String = "",
    val kernel: String = "",
    val arch: String = "",
    val virt: String = "",
    val cpu_name: String = "",
    val cpu_cores: Int = 0,
    val mem_total: Long = 0,
    val swap_total: Long = 0,
    val disk_total: Long = 0,
    val agent_version: String = "",
    val price: Double = 0.0,
    val currency: String = "",
    val billing_cycle: String = "",
    val expires_at: String? = null,
    val expires_in: Long? = null,
    val traffic_limit: Long = 0,
    val traffic_mode: String = "",
    val traffic_reset_day: Int = 0,
    val total_rx: Long = 0,
    val total_tx: Long = 0,
    val month_rx: Long = 0,
    val month_tx: Long = 0,
    val month_used: Long = 0,
    val month_start: String = "",
    val day_rx: Long = 0,
    val day_tx: Long = 0
) {
    fun toDomain() = Node(
        id = id,
        name = name,
        country = country,
        group = group,
        publicRemark = public_remark,
        sort = sort,
        public = public,
        online = online,
        lastSeenAgo = last_seen_ago,
        metrics = metrics?.toDomain(),
        os = os,
        kernel = kernel,
        arch = arch,
        virt = virt,
        cpuName = cpu_name,
        cpuCores = cpu_cores,
        memTotal = mem_total,
        swapTotal = swap_total,
        diskTotal = disk_total,
        agentVersion = agent_version,
        price = price,
        currency = currency,
        billingCycle = billing_cycle,
        expiresAt = expires_at,
        expiresIn = expires_in,
        trafficLimit = traffic_limit,
        trafficMode = traffic_mode,
        totalRx = total_rx,
        totalTx = total_tx,
        monthRx = month_rx,
        monthTx = month_tx,
        monthUsed = month_used,
        dayRx = day_rx,
        dayTx = day_tx
    )
}

/** 历史指标行（GET /api/nodes/{id}/metrics） */
@Serializable
data class MetricPointDto(
    val ts: Long = 0,
    val cpu: Double = 0.0,
    val mem_used: Long = 0,
    val disk_used: Long = 0,
    val net_rx: Long = 0,
    val net_tx: Long = 0
) {
    fun toDomain() = MetricPoint(
        ts = ts,
        cpu = cpu,
        memUsed = mem_used,
        diskUsed = disk_used,
        netRx = net_rx,
        netTx = net_tx
    )
}

@Serializable
data class MetricsHistoryDto(
    val metrics: List<MetricPointDto> = emptyList(),
    val step: Long = 60
) {
    fun toDomain(memTotal: Long, diskTotal: Long) = MetricsHistory(
        points = metrics.map { it.toDomain() },
        step = step,
        memTotal = memTotal,
        diskTotal = diskTotal
    )
}

/** GET /api/me */
@Serializable
data class MeDto(
    val authed: Boolean = false,
    val site_name: String = "Monitor",
    @SerialName("public_page") val publicPage: Boolean = false,
    val history_days: Int = 30,
    val site: String = ""
)

/** POST /api/auth/login 请求体 */
@Serializable
data class LoginBody(val password: String)

/** POST /api/auth/login 成功体 */
@Serializable
data class LoginOkDto(val ok: Boolean = false)
