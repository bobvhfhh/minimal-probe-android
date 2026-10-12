package com.minprobe.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Complete preset schemes make every color choice visibly affect the whole UI. */
enum class ThemeColor(
    val key: String,
    val seed: Color,
    val lightScheme: ColorScheme?,
    val darkScheme: ColorScheme?
) {
    DYNAMIC("dynamic", Color(0xFF006B5E), null, null),
    TEAL("teal", Color(0xFF006B5E), scheme(0xFF006B5E, 0xFF7AF7E0, 0xFFFAFDFA, 0xFF4A635D, 0xFF436278), darkScheme(0xFF5CDAC4, 0xFF005046, 0xFF191C1B, 0xFFB1CCC4, 0xFFAACBE3)),
    BLUE("blue", Color(0xFF1565C0), scheme(0xFF1565C0, 0xFFD4E3FF, 0xFFFDFBFF, 0xFF555F71, 0xFF6E5676), darkScheme(0xFFA5C8FF, 0xFF004A85, 0xFF1B1B1F, 0xFFBDC7DB, 0xFFDBBCE2)),
    PURPLE("purple", Color(0xFF7B1FA2), scheme(0xFF7B1FA2, 0xFFF1DAFF, 0xFFFFFBFF, 0xFF665A6F, 0xFF805159), darkScheme(0xFFDEB7FF, 0xFF620089, 0xFF1E1A20, 0xFFD1C2DB, 0xFFF3B7BF)),
    PINK("pink", Color(0xFFC2185B), scheme(0xFFC2185B, 0xFFFFD9E1, 0xFFFFFBFF, 0xFF75565D, 0xFF7C5635), darkScheme(0xFFFFB1C4, 0xFF8E0044, 0xFF201A1C, 0xFFE4BDC5, 0xFFEFBD94)),
    ORANGE("orange", Color(0xFFBF360C), scheme(0xFFBF360C, 0xFFFFDBCF, 0xFFFFFBFF, 0xFF77574C, 0xFF6B5E2F), darkScheme(0xFFFFB59C, 0xFF862200, 0xFF201A17, 0xFFE7BEAF, 0xFFD7C68D)),
    GREEN("green", Color(0xFF2E7D32), scheme(0xFF2E7D32, 0xFFA8F5AB, 0xFFFBFDF7, 0xFF52634F, 0xFF38656A), darkScheme(0xFF8DDC91, 0xFF126C25, 0xFF191D18, 0xFFB9CCB2, 0xFFA0CED3));

    companion object {
        fun fromKey(key: String): ThemeColor = entries.firstOrNull { it.key == key } ?: DYNAMIC
    }
}

private fun scheme(primary: Long, primaryContainer: Long, background: Long, secondary: Long, tertiary: Long) = lightColorScheme(
    primary = Color(primary), onPrimary = Color.White, primaryContainer = Color(primaryContainer), onPrimaryContainer = Color(0xFF202020),
    secondary = Color(secondary), onSecondary = Color.White, secondaryContainer = Color(primaryContainer), onSecondaryContainer = Color(0xFF202020),
    tertiary = Color(tertiary), onTertiary = Color.White, tertiaryContainer = Color(primaryContainer), onTertiaryContainer = Color(0xFF202020),
    background = Color(background), onBackground = Color(0xFF1B1B1B), surface = Color(background), onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(primaryContainer), onSurfaceVariant = Color(0xFF4B4B4B), outline = Color(0xFF777777), outlineVariant = Color(0xFFCACACA),
    error = Color(0xFFBA1A1A), errorContainer = Color(0xFFFFDAD6)
)

private fun darkScheme(primary: Long, primaryContainer: Long, background: Long, secondary: Long, tertiary: Long) = darkColorScheme(
    primary = Color(primary), onPrimary = Color(0xFF202020), primaryContainer = Color(primaryContainer), onPrimaryContainer = Color(primary),
    secondary = Color(secondary), onSecondary = Color(0xFF202020), secondaryContainer = Color(0xFF3A3A3A), onSecondaryContainer = Color(secondary),
    tertiary = Color(tertiary), onTertiary = Color(0xFF202020), tertiaryContainer = Color(0xFF3A3A3A), onTertiaryContainer = Color(tertiary),
    background = Color(background), onBackground = Color(0xFFE8E8E8), surface = Color(background), onSurface = Color(0xFFE8E8E8),
    surfaceVariant = Color(0xFF444444), onSurfaceVariant = Color(0xFFCCCCCC), outline = Color(0xFF999999), outlineVariant = Color(0xFF444444),
    error = Color(0xFFFFB4AB), errorContainer = Color(0xFF93000A)
)

val OnlineGreen = Color(0xFF2E7D32)
val OfflineRed = Color(0xFFC62828)
