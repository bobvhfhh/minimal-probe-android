package com.minprobe.app.ui.screen.nodedetail

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.minprobe.app.domain.model.MetricsHistory
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 历史图表组：CPU / 内存 / 磁盘 / 网络 */
@Composable
fun HistoryCharts(
    history: MetricsHistory,
    cpuLabel: String,
    memLabel: String,
    diskLabel: String,
    netLabel: String,
    animationEnabled: Boolean = true
) {
    val points = history.points
    if (points.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        val timeFmt = remember(history.step) { TimeAxisFormatter(points.map { it.ts }) }

        MetricChart(
            title = cpuLabel,
            values = listOf(points.map { it.cpu.toDouble() }),
            valueFormatter = { _, value, _ -> "${"%.0f".format(Locale.US, value)}%" },
            timeFormatter = timeFmt,
            color = MaterialTheme.colorScheme.primary,
            animationEnabled = animationEnabled
        )
        Spacer(Modifier.height(12.dp))

        if (history.memTotal > 0) {
            MetricChart(
                title = memLabel,
                values = listOf(
                    points.map { it.memUsed.toDouble() / history.memTotal * 100 }
                ),
                valueFormatter = { _, value, _ -> "${"%.0f".format(Locale.US, value)}%" },
                timeFormatter = timeFmt,
                color = MaterialTheme.colorScheme.secondary,
                animationEnabled = animationEnabled
            )
            Spacer(Modifier.height(12.dp))
        }

        if (history.diskTotal > 0) {
            MetricChart(
                title = diskLabel,
                values = listOf(
                    points.map { it.diskUsed.toDouble() / history.diskTotal * 100 }
                ),
                valueFormatter = { _, value, _ -> "${"%.0f".format(Locale.US, value)}%" },
                timeFormatter = timeFmt,
                color = MaterialTheme.colorScheme.tertiary,
                animationEnabled = animationEnabled
            )
            Spacer(Modifier.height(12.dp))
        }

        MetricChart(
            title = netLabel,
            values = listOf(
                points.map { it.netTx.toDouble() },
                points.map { it.netRx.toDouble() }
            ),
            valueFormatter = { _, value, _ -> formatRateCompact(value) },
            timeFormatter = timeFmt,
            color = MaterialTheme.colorScheme.primary,
            secondColor = MaterialTheme.colorScheme.tertiary,
            animationEnabled = animationEnabled
        )
    }
}

@Composable
private fun MetricChart(
    title: String,
    values: List<List<Double>>,
    valueFormatter: CartesianValueFormatter,
    timeFormatter: TimeAxisFormatter,
    color: Color,
    secondColor: Color? = null,
    animationEnabled: Boolean = true
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values) {
        modelProducer.runTransaction {
            lineSeries { values.forEach { series(it) } }
        }
    }
    val line1 = rememberThemedLine(color)
    val line2 = secondColor?.let { rememberThemedLine(it) }
    val lines = remember(line1, line2) {
        if (line2 != null) LineCartesianLayer.LineProvider.series(line1, line2)
        else LineCartesianLayer.LineProvider.series(line1)
    }
    val xFormatter = remember(timeFormatter) {
        CartesianValueFormatter { _, value, _ ->
            timeFormatter.format(value.toInt())
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(lines),
                startAxis = VerticalAxis.rememberStart(valueFormatter = valueFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = xFormatter)
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(horizontal = 8.dp),
            animationSpec = if (animationEnabled) {
                tween(durationMillis = 300, easing = LinearEasing)
            } else {
                null
            },
            animateIn = animationEnabled
        )
    }
}

@Composable
private fun rememberThemedLine(color: Color): LineCartesianLayer.Line =
    LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(color)),
        areaFill = LineCartesianLayer.AreaFill.single(
            Fill(Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), Color.Transparent)))
        )
    )

/** X 轴：点索引 → 时间标签（按跨度选格式） */
class TimeAxisFormatter(timestamps: List<Long>) {
    private val stamps: List<Long> = timestamps
    private val hourFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dayFmt = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    private val useDayFormat: Boolean = if (stamps.size >= 2) {
        (stamps.last() - stamps.first()) > 24 * 3600
    } else {
        false
    }

    fun format(index: Int): String {
        val ts = stamps.getOrNull(index) ?: return ""
        val fmt = if (useDayFormat) dayFmt else hourFmt
        return fmt.format(Date(ts * 1000))
    }
}

private fun formatRateCompact(bytesPerSec: Double): String {
    if (bytesPerSec < 1024) return "${bytesPerSec.toLong()}B/s"
    val units = arrayOf("KB/s", "MB/s", "GB/s")
    var value = bytesPerSec / 1024
    var i = 0
    while (value >= 1024 && i < units.size - 1) {
        value /= 1024
        i++
    }
    return "${"%.1f".format(Locale.US, value)}${units[i]}"
}
