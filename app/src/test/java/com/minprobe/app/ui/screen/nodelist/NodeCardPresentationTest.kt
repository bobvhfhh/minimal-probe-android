package com.minprobe.app.ui.screen.nodelist

import org.junit.Assert.assertEquals
import org.junit.Test

class NodeCardPresentationTest {
    @Test
    fun metadataLineKeepsLocationAndPlatformTogether() {
        assertEquals(
            "JP · VPS · Debian GNU/Linux 13",
            nodeMetadataLine("jp", "VPS", "Debian GNU/Linux 13")
        )
    }

    @Test
    fun metadataLineOmitsEmptyValuesAndHandlesBlankMetadata() {
        assertEquals("JP · Debian GNU/Linux 13", nodeMetadataLine("jp", "", "Debian GNU/Linux 13"))
        assertEquals("", nodeMetadataLine("", "", ""))
    }

    @Test
    fun agentBadgeNormalizesVersionPrefix() {
        assertEquals("V4", nodeAgentBadge("4"))
        assertEquals("V4", nodeAgentBadge("v4"))
        assertEquals("", nodeAgentBadge(""))
    }
}
