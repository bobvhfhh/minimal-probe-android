package com.minprobe.app.ui.screen.ping

import com.minprobe.app.domain.model.PingHistory
import com.minprobe.app.domain.model.PingPoint
import com.minprobe.app.domain.model.PingTask
import org.junit.Assert.assertEquals
import org.junit.Test

class PingPresentationTest {
    @Test
    fun latencyAndLossMapToClearConnectionStates() {
        assertEquals(PingHealth.HEALTHY, pingHealth(latencyMs = 24, lossPercent = 0.0))
        assertEquals(PingHealth.WARNING, pingHealth(latencyMs = 120, lossPercent = 0.0))
        assertEquals(PingHealth.ERROR, pingHealth(latencyMs = 260, lossPercent = 0.0))
        assertEquals(PingHealth.WARNING, pingHealth(latencyMs = 24, lossPercent = 0.5))
        assertEquals(PingHealth.ERROR, pingHealth(latencyMs = 24, lossPercent = 3.0))
        assertEquals(PingHealth.ERROR, pingHealth(latencyMs = null, lossPercent = 100.0))
    }

    @Test
    fun mostRecentLatencyAndWindowLossAreFormattedForCards() {
        assertEquals("24 ms", formatPingLatency(24))
        assertEquals("超时", formatPingLatency(null))
        assertEquals("0.0%", formatPacketLoss(0.0))
        assertEquals("2.4%", formatPacketLoss(2.44))
    }

    @Test
    fun taskValidationRequiresAReachableTcpTargetAndSafeInterval() {
        assertEquals(null, validatePingTask("Cloudflare", "1.1.1.1:443", 60))
        assertEquals(null, validatePingTask("IPv6", "[2606:4700:4700::1111]:443", 5))
        assertEquals("目标请填写 host:port，例如 1.1.1.1:443", validatePingTask("x", "1.1.1.1", 60))
        assertEquals("间隔需为 5–3600 秒", validatePingTask("x", "1.1.1.1:443", 4))
    }

    @Test
    fun summaryUsesLatestSampleAndAccurateWindowLossForEachTask() {
        val summaries = summarizePingHistory(
            PingHistory(
                taskNames = linkedMapOf(3L to "联通", 7L to "移动"),
                points = listOf(
                    PingPoint(3L, 100, 42, 0.0),
                    PingPoint(3L, 160, 57, 0.0),
                    PingPoint(7L, 160, null, 100.0)
                ),
                totalLossPercent = mapOf(7L to 3.6)
            )
        )

        assertEquals(PingSummary(3L, "联通", 57, 0.0, true), summaries[0])
        assertEquals(PingSummary(7L, "移动", null, 3.6, true), summaries[1])
    }

    @Test
    fun configuredTaskRemainsVisibleBeforeTheAgentReturnsItsFirstSample() {
        val summaries = summarizePingTasks(
            tasks = listOf(PingTask(12, "电信", "example.com:443", 60, listOf(8))),
            nodeId = 8,
            history = PingHistory()
        )

        assertEquals(PingSummary(12, "电信", null, 0.0, false), summaries.single())
    }
}
