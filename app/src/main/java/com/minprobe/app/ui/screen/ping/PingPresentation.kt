package com.minprobe.app.ui.screen.ping

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
