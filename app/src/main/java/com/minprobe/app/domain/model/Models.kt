package com.minprobe.app.domain.model

/** 面板认证方式：应急密码 / 访客（公开页） */
enum class AuthType {
    PASSWORD,
    GUEST
}

/** 用户添加的探针面板实例 */
data class ServerInstance(
    val id: Long = 0,
    val name: String = "",
    val baseUrl: String = "",
    /** 应急密码（内存中为明文，落盘时加密） */
    val password: String = "",
    /** monitor_session cookie 值（内存中为明文，落盘时加密） */
    val sessionCookie: String? = null,
    val authType: AuthType = AuthType.PASSWORD,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    /** 从 /api/me 读取的站点名与历史保留天数 */
    val siteName: String = "",
    val historyDays: Int = 30
)

/** 节点实时指标 */
data class NodeMetrics(
    val uptime: Long = 0,
    /** CPU 使用率 0~100 */
    val cpu: Double = 0.0,
    val load1: Double = 0.0,
    val load5: Double = 0.0,
    val load15: Double = 0.0,
    val memTotal: Long = 0,
    val memUsed: Long = 0,
    val swapTotal: Long = 0,
    val swapUsed: Long = 0,
    val diskTotal: Long = 0,
    val diskUsed: Long = 0,
    /** 当前速率 bytes/s */
    val netRx: Long = 0,
    val netTx: Long = 0,
    val tcp: Long = 0,
    val udp: Long = 0,
    val procs: Long = 0
)

/** 探针节点 */
data class Node(
    val id: Long,
    val name: String,
    val country: String = "",
    val group: String = "",
    val publicRemark: String = "",
    val sort: Int = 0,
    val public: Boolean = false,
    val online: Boolean = false,
    /** hub 时钟下的离线秒数，null 表示从未上线 */
    val lastSeenAgo: Long? = null,
    val metrics: NodeMetrics? = null,
    val os: String = "",
    val kernel: String = "",
    val arch: String = "",
    val virt: String = "",
    val cpuName: String = "",
    val cpuCores: Int = 0,
    val memTotal: Long = 0,
    val swapTotal: Long = 0,
    val diskTotal: Long = 0,
    val agentVersion: String = "",
    val price: Double = 0.0,
    val currency: String = "",
    val billingCycle: String = "",
    val expiresAt: String? = null,
    /** 到期剩余天数（hub 日历计算） */
    val expiresIn: Long? = null,
    val trafficLimit: Long = 0,
    val trafficMode: String = "",
    val totalRx: Long = 0,
    val totalTx: Long = 0,
    val monthRx: Long = 0,
    val monthTx: Long = 0,
    val monthUsed: Long = 0,
    val dayRx: Long = 0,
    val dayTx: Long = 0
) {
    private val effMemTotal: Long get() = metrics?.memTotal?.takeIf { it > 0 } ?: memTotal
    private val effDiskTotal: Long get() = metrics?.diskTotal?.takeIf { it > 0 } ?: diskTotal

    val memPercent: Double
        get() = if (effMemTotal > 0) (metrics?.memUsed ?: 0).toDouble() / effMemTotal * 100 else 0.0

    val diskPercent: Double
        get() = if (effDiskTotal > 0) (metrics?.diskUsed ?: 0).toDouble() / effDiskTotal * 100 else 0.0

    val totalTraffic: Long get() = totalRx + totalTx
    val monthTraffic: Long get() = monthRx + monthTx
    val dayTraffic: Long get() = dayRx + dayTx
}

/** 历史指标点（/api/nodes/{id}/metrics 行） */
data class MetricPoint(
    /** 秒级时间戳 */
    val ts: Long,
    val cpu: Double,
    val memUsed: Long,
    val diskUsed: Long,
    /** 区间内平均速率 bytes/s（hub 已按分钟/小时聚合） */
    val netRx: Long,
    val netTx: Long
)

/** 历史查询结果 */
data class MetricsHistory(
    val points: List<MetricPoint>,
    /** 每点跨度秒数 */
    val step: Long,
    val memTotal: Long = 0,
    val diskTotal: Long = 0
)
