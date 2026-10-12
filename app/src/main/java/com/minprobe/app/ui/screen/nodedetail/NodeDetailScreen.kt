package com.minprobe.app.ui.screen.nodedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.minprobe.app.R
import com.minprobe.app.domain.model.Node
import com.minprobe.app.domain.model.PingPoint
import com.minprobe.app.ui.screen.HandleEffects
import com.minprobe.app.ui.screen.formatBytes
import com.minprobe.app.ui.screen.formatExpiry
import com.minprobe.app.ui.screen.formatSpeed
import com.minprobe.app.ui.screen.formatUptime
import com.minprobe.app.ui.screen.server.ServerReLoginScreen
import com.minprobe.app.ui.screen.showToast
import com.minprobe.app.ui.screen.ping.PingHealth
import com.minprobe.app.ui.screen.ping.formatPacketLoss
import com.minprobe.app.ui.screen.ping.formatPingLatency
import com.minprobe.app.ui.screen.ping.pingHealth
import com.minprobe.app.ui.screen.ping.summarizePingTasks
import java.util.Locale
import org.koin.core.parameter.parametersOf

data class NodeDetailScreen(val nodeId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val viewModel = koinScreenModel<NodeDetailViewModel> { parametersOf(nodeId) }
        val state by viewModel.state.collectAsStateWithLifecycle()
        val prefsRepo: com.minprobe.app.data.local.preferences.UserPreferencesRepository = org.koin.compose.koinInject()
        val prefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(
            initialValue = com.minprobe.app.data.local.preferences.UserPreferences()
        )

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> viewModel.onScreenStarted()
                    Lifecycle.Event.ON_STOP -> viewModel.onScreenStopped()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                viewModel.onScreenStopped()
            }
        }

        HandleEffects(viewModel.effect) { effect ->
            when (effect) {
                is NodeDetailContract.Effect.ShowToast -> showToast(context, effect.message)
                is NodeDetailContract.Effect.NavigateToRelogin ->
                    navigator.push(ServerReLoginScreen())
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(state.node?.name ?: stringResource(R.string.node_detail)) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when {
                    state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.error != null -> Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            viewModel.onEvent(NodeDetailContract.Event.Retry)
                        }) { Text(stringResource(R.string.action_retry)) }
                    }
                    state.node != null -> NodeDetailContent(
                        node = state.node!!,
                        state = state,
                        chartAnimationEnabled = prefs.chartAnimationEnabled,
                        onHoursSelected = {
                            viewModel.onEvent(NodeDetailContract.Event.HoursSelected(it))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NodeDetailContent(
    node: Node,
    state: NodeDetailContract.State,
    chartAnimationEnabled: Boolean,
    onHoursSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = if (node.online) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        node.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        modifier = Modifier.weight(1f).padding(end = 10.dp)
                    )
                    DetailStatusBadge(node.online)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    listOf(node.country, node.os, node.arch)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 实时指标卡
        node.metrics?.let { m ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.detail_overview),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    MetricRow(
                        stringResource(R.string.metric_cpu),
                        String.format(Locale.US, "%.1f%%", m.cpu)
                    )
                    MetricRow(
                        stringResource(R.string.metric_mem),
                        "${formatBytes(m.memUsed)} / ${formatBytes(m.memTotal)} " +
                            "(${String.format(Locale.US, "%.1f%%", node.memPercent)})"
                    )
                    if (m.swapTotal > 0) {
                        MetricRow(
                            stringResource(R.string.metric_swap),
                            "${formatBytes(m.swapUsed)} / ${formatBytes(m.swapTotal)}"
                        )
                    }
                    MetricRow(
                        stringResource(R.string.metric_disk),
                        "${formatBytes(m.diskUsed)} / ${formatBytes(m.diskTotal)} " +
                            "(${String.format(Locale.US, "%.1f%%", node.diskPercent)})"
                    )
                    MetricRow(
                        stringResource(R.string.metric_net),
                        "↓ ${formatSpeed(m.netRx)}  ↑ ${formatSpeed(m.netTx)}"
                    )
                    MetricRow(
                        stringResource(R.string.metric_load),
                        String.format(Locale.US, "%.2f / %.2f / %.2f", m.load1, m.load5, m.load15)
                    )
                    MetricRow(
                        stringResource(R.string.metric_uptime),
                        formatUptime(m.uptime)
                    )
                    MetricRow(
                        stringResource(R.string.metric_procs),
                        "${m.procs}"
                    )
                    MetricRow(
                        stringResource(R.string.metric_tcp),
                        "${m.tcp} / ${m.udp}"
                    )
                }
            }
        }

        // 流量卡
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.traffic_month_used),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                if (node.trafficLimit > 0) {
                    MetricRow(
                        stringResource(R.string.traffic_month_used),
                        "${formatBytes(node.monthUsed)} / ${formatBytes(node.trafficLimit)}"
                    )
                } else {
                    MetricRow(
                        stringResource(R.string.traffic_month_used),
                        formatBytes(node.monthUsed)
                    )
                }
                MetricRow(stringResource(R.string.traffic_total), formatBytes(node.totalTraffic))
                MetricRow(stringResource(R.string.traffic_day), formatBytes(node.dayTraffic))
                formatExpiry(context, node.expiresIn)?.let {
                    MetricRow(stringResource(R.string.expiry_title), it)
                }
            }
        }

        // 信息卡
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.detail_info),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                if (node.os.isNotBlank()) MetricRow(stringResource(R.string.info_os), node.os)
                if (node.kernel.isNotBlank()) MetricRow(stringResource(R.string.info_kernel), node.kernel)
                if (node.arch.isNotBlank()) MetricRow(stringResource(R.string.info_arch), node.arch)
                if (node.virt.isNotBlank()) MetricRow(stringResource(R.string.info_virt), node.virt)
                if (node.cpuName.isNotBlank()) {
                    MetricRow(
                        stringResource(R.string.info_cpu),
                        "${node.cpuName} × ${node.cpuCores}"
                    )
                }
                if (node.agentVersion.isNotBlank()) {
                    MetricRow(stringResource(R.string.info_agent), node.agentVersion)
                }
                if (node.country.isNotBlank()) {
                    MetricRow(stringResource(R.string.info_country), node.country)
                }
                if (node.group.isNotBlank()) {
                    MetricRow(stringResource(R.string.info_group), node.group)
                }
            }
        }

        // 历史图表
        Text(
            stringResource(R.string.detail_charts),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.availableHours, key = { it }) { hours ->
                FilterChip(
                    selected = state.selectedHours == hours,
                    onClick = { onHoursSelected(hours) },
                    label = { Text(hoursLabel(hours)) }
                )
            }
        }
        when {
            state.isHistoryLoading -> Box(
                Modifier.fillMaxWidth().height(200.dp)
            ) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            state.historyError != null -> Text(
                state.historyError!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
            state.history != null && state.history.points.isNotEmpty() ->
                HistoryCharts(
                    history = state.history,
                    cpuLabel = stringResource(R.string.metric_cpu),
                    memLabel = stringResource(R.string.metric_mem),
                    diskLabel = stringResource(R.string.metric_disk),
                    netLabel = stringResource(R.string.metric_net),
                    animationEnabled = chartAnimationEnabled
                )
            else -> Text(
                stringResource(R.string.chart_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }

        Text(
            "TCP 延迟监控",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp, end = 4.dp)
        )
        when {
            state.isPingLoading && state.configuredPingTasks.isEmpty() -> Box(Modifier.fillMaxWidth().height(92.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp).align(Alignment.Center))
            }
            state.pingHistory != null && (state.pingHistory!!.taskNames.isNotEmpty() || state.configuredPingTasks.isNotEmpty()) ->
                PingHistoryCard(state.pingHistory!!, state.configuredPingTasks, node.id)
            state.configuredPingTasks.isNotEmpty() ->
                PingHistoryCard(com.minprobe.app.domain.model.PingHistory(), state.configuredPingTasks, node.id)
            state.pingError != null -> Text(
                state.pingError!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
            else -> Text(
                "未配置 TCP 监控。请在节点列表右上角的延迟监控中添加任务。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PingHistoryCard(
    history: com.minprobe.app.domain.model.PingHistory,
    tasks: List<com.minprobe.app.domain.model.PingTask>,
    nodeId: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            summarizePingTasks(tasks, nodeId, history).forEach { summary ->
                val samples = history.points.filter { it.taskId == summary.taskId }.takeLast(24)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(summary.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(
                        if (summary.hasSamples) formatPingLatency(summary.latestLatencyMs) else "等待采样",
                        color = if (summary.hasSamples) pingHealth(summary.latestLatencyMs, summary.totalLossPercent).toColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (summary.hasSamples) formatPacketLoss(summary.totalLossPercent) else "—",
                        color = if (summary.hasSamples) pingHealth(summary.latestLatencyMs, summary.totalLossPercent).toColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                PingSegments(samples)
            }
        }
    }
}

@Composable
private fun PingSegments(samples: List<PingPoint>) {
    val latest = samples.takeLast(24)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(24) { index ->
            val sample = latest.getOrNull(index)
            val color = when (sample?.let { pingHealth(it.latencyMs, it.lossPercent) }) {
                PingHealth.HEALTHY -> androidx.compose.ui.graphics.Color(0xFF35C878)
                PingHealth.WARNING -> androidx.compose.ui.graphics.Color(0xFFF2B84B)
                PingHealth.ERROR -> androidx.compose.ui.graphics.Color(0xFFE24B4B)
                null -> MaterialTheme.colorScheme.surfaceVariant
            }
            androidx.compose.material3.Surface(
                modifier = Modifier.weight(1f).height(8.dp),
                shape = MaterialTheme.shapes.extraSmall,
                color = color
            ) {}
        }
    }
}

@Composable
private fun PingHealth.toColor(): androidx.compose.ui.graphics.Color = when (this) {
    PingHealth.HEALTHY -> androidx.compose.ui.graphics.Color(0xFF23B66F)
    PingHealth.WARNING -> androidx.compose.ui.graphics.Color(0xFFE2A52C)
    PingHealth.ERROR -> MaterialTheme.colorScheme.error
}

@Composable
private fun hoursLabel(hours: Int): String {
    return when (hours) {
        1 -> stringResource(R.string.chart_hours_1)
        6 -> stringResource(R.string.chart_hours_6)
        24 -> stringResource(R.string.chart_hours_24)
        168 -> stringResource(R.string.chart_hours_168)
        else -> "${hours}h"
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(0.62f)
        )
    }
}

@Composable
private fun DetailStatusBadge(online: Boolean) {
    Surface(
        color = if (online) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Text(
            if (online) "ONLINE" else "OFFLINE",
            style = MaterialTheme.typography.labelSmall,
            color = if (online) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
