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
import com.minprobe.app.ui.theme.OfflineRed
import com.minprobe.app.ui.theme.OnlineGreen
import java.util.Locale

@Composable
fun NodeCard(node: Node, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (node.online) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(online = node.online)
                Spacer(Modifier.width(8.dp))
                Text(
                    node.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = if (node.online) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        if (node.online) "ONLINE" else "OFFLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (node.online) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (node.country.isNotBlank()) {
                    Text(
                        node.country.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (node.group.isNotBlank() || node.os.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    listOf(node.group, node.os).filter { it.isNotBlank() }.joinToString(" · "),
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

            Spacer(Modifier.height(8.dp))
            MetricBar(
                label = stringResource(R.string.metric_cpu),
                valueText = String.format(Locale.US, "%.1f%%", metrics.cpu),
                fraction = (metrics.cpu / 100).toFloat().coerceIn(0f, 1f)
            )
            Spacer(Modifier.height(6.dp))
            MetricBar(
                label = stringResource(R.string.metric_mem),
                valueText = "${formatBytes(metrics.memUsed)} / ${formatBytes(metrics.memTotal)}",
                fraction = node.memPercent.toFloat() / 100
            )
            Spacer(Modifier.height(6.dp))
            MetricBar(
                label = stringResource(R.string.metric_disk),
                valueText = "${formatBytes(metrics.diskUsed)} / ${formatBytes(metrics.diskTotal)}",
                fraction = node.diskPercent.toFloat() / 100
            )
            Spacer(Modifier.height(8.dp))
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
        }
    }
}

@Composable
private fun StatusDot(online: Boolean) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .width(10.dp)
            .height(10.dp)
    ) {
        drawCircle(if (online) OnlineGreen else OfflineRed)
    }
}

@Composable
private fun MetricBar(label: String, valueText: String, fraction: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp)
        )
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(MaterialTheme.shapes.small)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            valueText,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}
