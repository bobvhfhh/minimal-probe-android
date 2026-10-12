package com.minprobe.app.ui.screen

import android.content.Context
import com.minprobe.app.R
import java.util.Locale
import kotlin.math.abs

/** 字节数 → 人性化字符串（B/KB/MB/GB/TB） */
fun formatBytes(bytes: Long): String {
    val absBytes = abs(bytes.toDouble())
    if (absBytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB", "PB")
    var value = absBytes / 1024
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024
        unitIndex++
    }
    val formatted = if (value >= 100) {
        String.format(Locale.US, "%.0f", value)
    } else {
        String.format(Locale.US, "%.1f", value)
    }
    return "$formatted ${units[unitIndex]}"
}

/** 速率 bytes/s → 人性化字符串 */
fun formatSpeed(bytesPerSec: Long): String = "${formatBytes(bytesPerSec)}/s"

/** 离线秒数（hub 时钟）→ 人性化字符串 */
fun formatAgo(context: Context, seconds: Long?): String {
    if (seconds == null) return context.getString(R.string.node_never_seen)
    if (seconds < 60) return context.getString(R.string.time_just_now)
    val minutes = seconds / 60
    if (minutes < 60) return context.getString(R.string.time_minutes_ago, minutes)
    val hours = minutes / 60
    if (hours < 24) return context.getString(R.string.time_hours_ago, hours)
    return context.getString(R.string.time_days_ago, hours / 24)
}

/** 秒 → uptime 文案（如 3d 4h） */
fun formatUptime(seconds: Long): String {
    if (seconds <= 0) return "0m"
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

/** 到期日期 "2026-12-01" → 剩余天数文案 */
fun formatExpiry(context: Context, expiresIn: Long?): String? {
    if (expiresIn == null) return null
    return if (expiresIn < 0) {
        context.getString(R.string.expiry_expired)
    } else {
        context.getString(R.string.expiry_remaining, expiresIn)
    }
}
