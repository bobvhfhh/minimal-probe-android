package com.minprobe.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeColor(val key: String, val seed: Color) {
    DYNAMIC("dynamic", GreenPrimary),
    GREEN("green", GreenPrimary),
    TEAL("teal", TealPrimary),
    BLUE("blue", BluePrimary),
    PURPLE("purple", PurplePrimary),
    ORANGE("orange", OrangePrimary);

    companion object {
        fun fromKey(key: String): ThemeColor =
            entries.firstOrNull { it.key == key } ?: DYNAMIC
    }
}

private fun seedScheme(seed: Color, dark: Boolean) =
    if (dark) darkColorScheme(primary = seed) else lightColorScheme(primary = seed)

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
        else -> seedScheme(themeColor.seed, darkTheme)
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
