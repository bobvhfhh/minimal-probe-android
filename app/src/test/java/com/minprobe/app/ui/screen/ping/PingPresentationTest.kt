package com.minprobe.app.ui.screen.ping

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
}
