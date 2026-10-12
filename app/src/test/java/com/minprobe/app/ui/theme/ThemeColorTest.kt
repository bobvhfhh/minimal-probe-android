package com.minprobe.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ThemeColorTest {
    @Test
    fun everyThemeHasDistinctSecondaryAccent() {
        assertNotEquals(ThemeColor.GREEN.seed, ThemeColor.BLUE.seed)
        assertNotEquals(ThemeColor.ORANGE.seed, ThemeColor.PURPLE.seed)
        assertEquals(ThemeColor.fromKey("purple"), ThemeColor.PURPLE)
    }

    @Test
    fun presetThemeProvidesCompleteLightAndDarkSchemes() {
        assertNotNull(ThemeColor.BLUE.lightScheme)
        assertNotNull(ThemeColor.BLUE.darkScheme)
    }
}
