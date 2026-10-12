package com.minprobe.app.ui.screen.ping

import com.minprobe.app.domain.model.PingHistory
import com.minprobe.app.domain.model.PingTask
import java.util.Locale

/** 连接质量的视觉状态：截图中的绿、黄、红分段条共用这套判定。 */
enum class PingHealth { HEALTHY, WARNING, ERROR }

internal fun pingHealth(latencyMs: Long?, lossPercent: Double): PingHealth = when {
    latencyMs == null || lossPercent >= 3.0 || latencyMs > 180L -> PingHealth.ERROR
    lossPercent > 0.0 || latencyMs > 80L -> PingHealth.WARNING
    else -> PingHealth.HEALTHY
}

internal fun formatPingLatency(latencyMs: Long?): String = latencyMs?.let { "$it ms" } ?: "超时"

internal fun formatPacketLoss(lossPercent: Double): String =
    String.format(Locale.US, "%.1f%%", lossPercent.coerceIn(0.0, 100.0))

/** 卡片只渲染服务端给出的任务及其最后一次结果，不在手机端猜测延迟。 */
internal data class PingSummary(
    val taskId: Long,
    val name: String,
    val latestLatencyMs: Long?,
    val totalLossPercent: Double,
    /** false 代表任务已下发，但 Agent 还没有回传首个结果。 */
    val hasSamples: Boolean
)

internal fun summarizePingHistory(history: PingHistory): List<PingSummary> =
    history.taskNames.map { (taskId, name) ->
        val sample = history.latestFor(taskId)
        PingSummary(
            taskId = taskId,
            name = name,
            latestLatencyMs = sample?.latencyMs,
            totalLossPercent = history.totalLossPercent[taskId] ?: 0.0,
            hasSamples = sample != null
        )
    }

/**
 * 管理员接口中的任务分配才是“已配置”的事实来源；历史接口仅代表已经回传的数据。
 * 两者合并，避免刚保存任务或节点刚上线时被错误提示为“未配置”。
 */
internal fun summarizePingTasks(
    tasks: List<PingTask>,
    nodeId: Long,
    history: PingHistory
): List<PingSummary> {
    val configured = tasks.filter { nodeId in it.nodeIds }
    val configuredIds = configured.map { it.id }.toSet()
    val names = buildList {
        configured.forEach { add(it.id to it.name) }
        history.taskNames.forEach { (id, name) -> if (id !in configuredIds) add(id to name) }
    }
    return names.map { (taskId, name) ->
        val sample = history.latestFor(taskId)
        PingSummary(
            taskId = taskId,
            name = name,
            latestLatencyMs = sample?.latencyMs,
            totalLossPercent = history.totalLossPercent[taskId] ?: 0.0,
            hasSamples = sample != null
        )
    }
}

/** 与 Hub 一致的前置校验，避免无效任务保存后长期显示 100% 丢包。 */
internal fun validatePingTask(name: String, target: String, intervalSeconds: Int): String? {
    if (name.isBlank() || target.isBlank()) return "请填写名称和目标地址"
    if (intervalSeconds !in 5..3600) return "间隔需为 5–3600 秒"
    val value = target.trim()
    val valid = if (value.startsWith("[")) {
        val separator = value.indexOf("]:")
        separator > 1 && value.substring(separator + 2).toIntOrNull()?.let { it in 1..65535 } == true
    } else {
        val separator = value.lastIndexOf(':')
        separator in 1 until value.lastIndex && !value.substring(0, separator).contains(':') &&
            value.substring(separator + 1).toIntOrNull()?.let { it in 1..65535 } == true
    }
    return if (valid) null else "目标请填写 host:port，例如 1.1.1.1:443"
}
