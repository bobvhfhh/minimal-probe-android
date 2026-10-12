package com.minprobe.app.ui.screen.nodelist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.minprobe.app.R
import com.minprobe.app.domain.model.Node
import com.minprobe.app.domain.model.PingHistory
import com.minprobe.app.domain.model.PingPoint
import com.minprobe.app.domain.model.PingTask
import com.minprobe.app.ui.screen.formatAgo
import com.minprobe.app.ui.screen.formatBytes
import com.minprobe.app.ui.screen.formatSpeed
import com.minprobe.app.ui.screen.ping.PingHealth
import com.minprobe.app.ui.screen.ping.formatPacketLoss
import com.minprobe.app.ui.screen.ping.formatPingLatency
import com.minprobe.app.ui.screen.ping.pingHealth
import com.minprobe.app.ui.screen.ping.summarizePingTasks
import java.util.Locale

internal fun nodeMetadataLine(country: String, group: String, os: String): String =
    listOf(country.uppercase(Locale.US), group, os).filter { it.isNotBlank() }.joinToString(" · ")

internal fun nodeAgentBadge(version: String): String = version.trim().removePrefix("v").removePrefix("V")
    .takeIf { it.isNotBlank() }?.let { "V$it" } ?: ""

@Composable
fun NodeCard(
    node: Node,
    pingTasks: List<PingTask> = emptyList(),
    pingHistory: PingHistory? = null,
    onClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            NodeHeader(node)
            if (!node.online) {
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.node_offline_ago, formatAgo(context, node.lastSeenAgo)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            val metrics = node.metrics ?: run {
                Spacer(Modifier.height(10.dp)); Text(stringResource(R.string.node_never_seen), color = MaterialTheme.colorScheme.onSurfaceVariant); return@Column
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                NodeMetric("CPU", String.format(Locale.US, "%.1f%%", metrics.cpu), "${node.cpuCores} 核", (metrics.cpu / 100).toFloat(), Modifier.weight(1f))
                NodeMetric(stringResource(R.string.metric_mem), String.format(Locale.US, "%.1f%%", node.memPercent), "${formatBytes(metrics.memUsed)} / ${formatBytes(metrics.memTotal)}", (node.memPercent / 100).toFloat(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(13.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                NodeMetric(stringResource(R.string.metric_disk), String.format(Locale.US, "%.1f%%", node.diskPercent), "${formatBytes(metrics.diskUsed)} / ${formatBytes(metrics.diskTotal)}", (node.diskPercent / 100).toFloat(), Modifier.weight(1f))
                NodeMetric(stringResource(R.string.metric_load), String.format(Locale.US, "%.2f", metrics.load1), "1 / 5 / 15 min", (metrics.load1 / 4).toFloat(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(15.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TrafficValue("↑ 上行", formatSpeed(metrics.netTx), "出站 ${formatBytes(node.totalTx)}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                TrafficValue("↓ 下行", formatSpeed(metrics.netRx), "入站 ${formatBytes(node.totalRx)}", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            }
            if (node.trafficLimit > 0) {
                Spacer(Modifier.height(12.dp))
                SegmentMeter("流量用量", "${formatBytes((node.trafficLimit - node.monthUsed).coerceAtLeast(0))} 剩余 · ${formatBytes(node.monthUsed)} / ${formatBytes(node.trafficLimit)}", node.monthUsed.toFloat() / node.trafficLimit)
            }
            val pingSummaries = summarizePingTasks(pingTasks, node.id, pingHistory ?: PingHistory())
            if (pingHistory != null || pingSummaries.isNotEmpty()) {
                Spacer(Modifier.height(13.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
                Spacer(Modifier.height(10.dp))
                Text("TCP 延迟", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                pingSummaries.forEach { summary ->
                    val samples = (pingHistory?.points ?: emptyList()).filter { it.taskId == summary.taskId }.takeLast(18)
                    PingStatusRow(summary.name, summary.latestLatencyMs, summary.totalLossPercent, summary.hasSamples, samples)
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(13.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FooterValue("TCP 连接", metrics.tcp.toString(), MaterialTheme.colorScheme.primary)
                FooterValue("UDP 连接", metrics.udp.toString(), MaterialTheme.colorScheme.secondary)
                FooterValue("到期", node.expiresIn?.let { "${it}天" } ?: "—", MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun PingStatusRow(
    name: String,
    latencyMs: Long?,
    lossPercent: Double,
    hasSamples: Boolean,
    samples: List<PingPoint>
) {
    val health = pingHealth(latencyMs, lossPercent)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(
            if (hasSamples) formatPingLatency(latencyMs) else "等待采样",
            style = MaterialTheme.typography.labelMedium,
            color = if (hasSamples) health.toPingColor() else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (hasSamples) formatPacketLoss(lossPercent) else "—",
            style = MaterialTheme.typography.labelMedium,
            color = if (hasSamples) health.toPingColor() else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(5.dp))
    PingSegments(samples)
}

@Composable
private fun PingSegments(samples: List<PingPoint>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(18) { index ->
            val sample = samples.getOrNull(index)
            val color = when (sample?.let { pingHealth(it.latencyMs, it.lossPercent) }) {
                PingHealth.HEALTHY -> Color(0xFF35C878)
                PingHealth.WARNING -> Color(0xFFF2B84B)
                PingHealth.ERROR -> Color(0xFFE24B4B)
                null -> MaterialTheme.colorScheme.surfaceVariant
            }
            Surface(
                modifier = Modifier.weight(1f).height(7.dp),
                shape = MaterialTheme.shapes.extraSmall,
                color = color
            ) {}
        }
    }
}

@Composable
private fun PingHealth.toPingColor(): Color = when (this) {
    PingHealth.HEALTHY -> Color(0xFF23B66F)
    PingHealth.WARNING -> Color(0xFFE2A52C)
    PingHealth.ERROR -> MaterialTheme.colorScheme.error
}

@Composable private fun NodeHeader(node: Node) {
    val indicatorColor = if (node.online) Color(0xFF249A3A) else MaterialTheme.colorScheme.error
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(12.dp)) { drawCircle(indicatorColor) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(node.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(node.os.ifBlank { nodeMetadataLine(node.country, node.group, "") }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        nodeAgentBadge(node.agentVersion).takeIf { it.isNotBlank() }?.let { AgentBadge(it) }
        Spacer(Modifier.width(7.dp))
        StatusBadge(node.online)
    }
}

@Composable private fun AgentBadge(text: String) = Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
    Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
}

@Composable private fun StatusBadge(online: Boolean) = Surface(
    color = if (online) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
    shape = MaterialTheme.shapes.large
) { Text(if (online) "ONLINE" else "OFFLINE", modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = if (online) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer) }

@Composable private fun NodeMetric(label: String, value: String, detail: String, progress: Float, modifier: Modifier) = Column(modifier) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
    Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(6.dp)); Segments(progress)
}

@Composable private fun TrafficValue(label: String, value: String, detail: String, color: Color, modifier: Modifier) = Column(modifier) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold, maxLines = 1)
    Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable private fun SegmentMeter(label: String, value: String, progress: Float) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, style = MaterialTheme.typography.labelMedium); Text(value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    Spacer(Modifier.height(6.dp)); Segments(progress)
}

/** 用绿、黄、红表达资源余量；已占用越多，颜色越接近风险色。 */
@Composable private fun Segments(progress: Float) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        val active = (progress.coerceIn(0f, 1f) * 18).toInt()
        repeat(18) { index ->
            val color = when {
                index >= active -> MaterialTheme.colorScheme.surfaceVariant
                index < 11 -> Color(0xFF35C878)
                index < 15 -> Color(0xFFF2B84B)
                else -> Color(0xFFE24B4B)
            }
            Surface(
                modifier = Modifier.weight(1f).height(7.dp),
                shape = MaterialTheme.shapes.extraSmall,
                color = color
            ) {}
        }
    }
}

@Composable private fun FooterValue(label: String, value: String, color: Color) = Column {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.SemiBold)
}
