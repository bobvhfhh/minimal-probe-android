package com.minprobe.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemeColorTest {
    @Test
    fun everyThemeHasDistinctSecondaryAccent() {
        assertNotEquals(ThemeColor.GREEN.secondary, ThemeColor.BLUE.secondary)
        assertNotEquals(ThemeColor.ORANGE.seed, ThemeColor.ORANGE.secondary)
        assertEquals(ThemeColor.fromKey("purple"), ThemeColor.PURPLE)
    }
}
