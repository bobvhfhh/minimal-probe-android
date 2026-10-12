package com.minprobe.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun MiniProbeTheme(
    themeColor: ThemeColor = ThemeColor.DYNAMIC,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        themeColor == ThemeColor.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        themeColor == ThemeColor.DYNAMIC ->
            if (darkTheme) ThemeColor.TEAL.darkScheme!! else ThemeColor.TEAL.lightScheme!!
        else -> if (darkTheme) themeColor.darkScheme!! else themeColor.lightScheme!!
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
