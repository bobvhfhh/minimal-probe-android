package com.minprobe.app.ui.screen.nodelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.minprobe.app.R
import com.minprobe.app.domain.model.Node
import com.minprobe.app.ui.screen.formatAgo
import com.minprobe.app.ui.screen.formatBytes
import com.minprobe.app.ui.screen.formatSpeed
import java.util.Locale

internal fun nodeMetadataLine(country: String, group: String, os: String): String =
    listOf(country.uppercase(Locale.US), group, os)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

@Composable
fun NodeCard(node: Node, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (node.online) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    node.country.ifBlank { "•" }.uppercase(Locale.US),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    node.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 10.dp)
                )
                StatusBadge(online = node.online)
            }

            if (node.country.isNotBlank() || node.group.isNotBlank() || node.os.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    nodeMetadataLine(node.country, node.group, node.os),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!node.online) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(
                        R.string.node_offline_ago,
                        formatAgo(context, node.lastSeenAgo)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            val metrics = node.metrics
            if (metrics == null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.node_never_seen),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CompactMetric(stringResource(R.string.metric_cpu), String.format(Locale.US, "%.1f%%", metrics.cpu), (metrics.cpu / 100).toFloat(), Modifier.weight(1f))
                CompactMetric(stringResource(R.string.metric_mem), "${node.memPercent.toInt()}%", (node.memPercent / 100).toFloat(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CompactMetric(stringResource(R.string.metric_disk), "${node.diskPercent.toInt()}%", (node.diskPercent / 100).toFloat(), Modifier.weight(1f))
                CompactMetric(stringResource(R.string.metric_load), String.format(Locale.US, "%.2f", metrics.load1), (metrics.load1 / 4).toFloat(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(7.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "↓ ${formatSpeed(metrics.netRx)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "↑ ${formatSpeed(metrics.netTx)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (node.trafficLimit > 0) {
                    Text(
                        "${formatBytes(node.monthUsed)} / ${formatBytes(node.trafficLimit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("TCP ${metrics.tcp} · UDP ${metrics.udp}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (node.expiresIn != null) "${node.expiresIn}天" else "—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun StatusBadge(online: Boolean) {
    val foreground = if (online) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Surface(
        color = if (online) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier.width(6.dp).height(6.dp)
            ) {
                drawCircle(foreground)
            }
            Text(
                if (online) "ONLINE" else "OFFLINE",
                style = MaterialTheme.typography.labelSmall,
                color = if (online) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            )
        }
    }
}

@Composable
private fun CompactMetric(label: String, valueText: String, fraction: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(MaterialTheme.shapes.small)
        )
        Text(
            valueText,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}
