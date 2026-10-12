package com.minprobe.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by
    preferencesDataStore(name = "user_preferences")

/** 用户偏好（主题 / 语言 / 安全开关），明文存 DataStore，无敏感数据 */
class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val THEME_COLOR_KEY = stringPreferencesKey("theme_color")
        private val DARK_MODE_KEY = stringPreferencesKey("dark_mode")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val FONT_SCALE_KEY = floatPreferencesKey("font_scale")
        private val CHART_ANIMATION_KEY = booleanPreferencesKey("chart_animation")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
        private val NODE_SORT_KEY = stringPreferencesKey("node_sort")
    }

    val preferencesFlow: Flow<UserPreferences> =
        context.dataStore.data.map { prefs ->
            UserPreferences(
                themeColorKey = prefs[THEME_COLOR_KEY] ?: "dynamic",
                darkModeKey = prefs[DARK_MODE_KEY] ?: "system",
                languageKey = prefs[LANGUAGE_KEY] ?: "system",
                fontScale = prefs[FONT_SCALE_KEY] ?: 1.0f,
                chartAnimationEnabled = prefs[CHART_ANIMATION_KEY] ?: true,
                biometricEnabled = prefs[BIOMETRIC_ENABLED_KEY] ?: false,
                nodeSortKey = prefs[NODE_SORT_KEY] ?: "default"
            )
        }

    suspend fun setThemeColor(key: String) {
        context.dataStore.edit { it[THEME_COLOR_KEY] = key }
    }

    suspend fun setDarkMode(key: String) {
        context.dataStore.edit { it[DARK_MODE_KEY] = key }
    }

    suspend fun setLanguage(key: String) {
        context.dataStore.edit { it[LANGUAGE_KEY] = key }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[FONT_SCALE_KEY] = scale }
    }

    suspend fun setChartAnimation(enabled: Boolean) {
        context.dataStore.edit { it[CHART_ANIMATION_KEY] = enabled }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[BIOMETRIC_ENABLED_KEY] = enabled }
    }

    suspend fun setNodeSort(key: String) {
        context.dataStore.edit { it[NODE_SORT_KEY] = key }
    }
}

data class UserPreferences(
    val themeColorKey: String = "dynamic",
    val darkModeKey: String = "system",
    val languageKey: String = "system",
    val fontScale: Float = 1.0f,
    val chartAnimationEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val nodeSortKey: String = "default"
)
