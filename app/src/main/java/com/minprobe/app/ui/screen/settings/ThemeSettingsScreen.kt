package com.minprobe.app.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.minprobe.app.R
import com.minprobe.app.data.local.preferences.UserPreferences
import com.minprobe.app.data.local.preferences.UserPreferencesRepository
import com.minprobe.app.ui.theme.ThemeColor
import org.koin.compose.koinInject

class ThemeSettingsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<SettingsViewModel>()
        val prefsRepo: UserPreferencesRepository = koinInject()
        val prefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(
            initialValue = UserPreferences()
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_appearance)) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    stringResource(R.string.settings_theme_color),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                ThemeColor.entries.forEach { theme ->
                    OptionRow(
                        color = theme.seed,
                        selected = prefs.themeColorKey == theme.key,
                        label = themeLabel(theme),
                        onClick = {
                            viewModel.onEvent(SettingsContract.Event.SetThemeColor(theme.key))
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.settings_dark_mode),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                listOf(
                    "system" to stringResource(R.string.settings_dark_mode_system),
                    "light" to stringResource(R.string.settings_dark_mode_light),
                    "dark" to stringResource(R.string.settings_dark_mode_dark)
                ).forEach { (key, label) ->
                    OptionRow(
                        selected = prefs.darkModeKey == key,
                        label = label,
                        onClick = {
                            viewModel.onEvent(SettingsContract.Event.SetDarkMode(key))
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                listOf(
                    "system" to stringResource(R.string.settings_language_system),
                    "zh" to "中文",
                    "en" to "English"
                ).forEach { (key, label) ->
                    OptionRow(
                        selected = prefs.languageKey == key,
                        label = label,
                        onClick = {
                            viewModel.onEvent(SettingsContract.Event.SetLanguage(key))
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "${stringResource(R.string.settings_font_scale)}: ${(prefs.fontScale * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = prefs.fontScale,
                    onValueChange = {
                        viewModel.onEvent(SettingsContract.Event.SetFontScale(it))
                    },
                    valueRange = 0.8f..1.3f,
                    steps = 9
                )
            }
        }
    }
}

private fun themeLabel(theme: ThemeColor): String {
    return when (theme) {
        ThemeColor.DYNAMIC -> "Dynamic"
        ThemeColor.GREEN -> "Green"
        ThemeColor.TEAL -> "Teal"
        ThemeColor.BLUE -> "Blue"
        ThemeColor.PURPLE -> "Purple"
        ThemeColor.ORANGE -> "Orange"
    }
}

@Composable
private fun OptionRow(selected: Boolean, label: String, color: androidx.compose.ui.graphics.Color? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        color?.let {
            Spacer(Modifier.size(12.dp).background(it, MaterialTheme.shapes.small))
            Spacer(Modifier.size(4.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
