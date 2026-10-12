package com.minprobe.app.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.minprobe.app.R
import com.minprobe.app.data.local.preferences.UserPreferences
import com.minprobe.app.data.local.preferences.UserPreferencesRepository
import com.minprobe.app.ui.screen.HandleEffects
import com.minprobe.app.ui.screen.showToast
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsHubScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val viewModel = koinScreenModel<SettingsViewModel>()
        val prefsRepo: UserPreferencesRepository = koinInject()
        val prefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(
            initialValue = UserPreferences()
        )

        var showExportWarning by remember { mutableStateOf(false) }

        val exportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri -> if (uri != null) viewModel.exportConfig(uri) }
        val importLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri -> if (uri != null) viewModel.importConfig(uri) }

        HandleEffects(viewModel.effect) { effect ->
            when (effect) {
                is SettingsContract.Effect.ShowToast -> showToast(context, effect.message)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
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
                    .padding(16.dp)
            ) {
                SectionHeader(stringResource(R.string.settings_appearance))
                SettingRow(
                    title = stringResource(R.string.settings_theme_color),
                    onClick = { navigator.push(ThemeSettingsScreen()) }
                )
                SettingRow(
                    title = stringResource(R.string.settings_dark_mode),
                    subtitle = darkModeLabel(prefs.darkModeKey),
                    onClick = { navigator.push(ThemeSettingsScreen()) }
                )
                SettingRow(
                    title = stringResource(R.string.settings_language),
                    subtitle = languageLabel(prefs.languageKey),
                    onClick = { navigator.push(ThemeSettingsScreen()) }
                )
                SettingRow(
                    title = stringResource(R.string.settings_font_scale),
                    subtitle = "${(prefs.fontScale * 100).toInt()}%",
                    onClick = { navigator.push(ThemeSettingsScreen()) }
                )
                SwitchRow(
                    title = stringResource(R.string.settings_chart_animation),
                    checked = prefs.chartAnimationEnabled,
                    onCheckedChange = {
                        viewModel.onEvent(SettingsContract.Event.SetChartAnimation(it))
                    }
                )

                Spacer(Modifier.height(16.dp))
                SectionHeader(stringResource(R.string.settings_security))
                SwitchRow(
                    title = stringResource(R.string.settings_biometric),
                    subtitle = stringResource(R.string.settings_biometric_desc),
                    checked = prefs.biometricEnabled,
                    onCheckedChange = {
                        viewModel.onEvent(SettingsContract.Event.SetBiometricEnabled(it))
                    }
                )

                Spacer(Modifier.height(16.dp))
                SectionHeader(stringResource(R.string.settings_backup))
                // 审查修复：导出前明确告知是明文（含密码）
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_export_plaintext_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                SettingRow(
                    title = stringResource(R.string.settings_export_config),
                    subtitle = stringResource(R.string.settings_export_config_desc),
                    onClick = { showExportWarning = true }
                )
                SettingRow(
                    title = stringResource(R.string.settings_import_config),
                    subtitle = stringResource(R.string.settings_import_config_desc),
                    onClick = { importLauncher.launch(arrayOf("application/json")) }
                )

                Spacer(Modifier.height(16.dp))
                SectionHeader(stringResource(R.string.settings_about))
                SettingRow(
                    title = stringResource(R.string.settings_about),
                    onClick = { navigator.push(AboutScreen()) }
                )
            }
        }

        if (showExportWarning) {
            AlertDialog(
                onDismissRequest = { showExportWarning = false },
                title = { Text(stringResource(R.string.settings_export_config)) },
                text = { Text(stringResource(R.string.settings_export_plaintext_warning)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExportWarning = false
                            val name = "minprobe-backup-${
                                SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                            }.json"
                            exportLauncher.launch(name)
                        }
                    ) { Text(stringResource(R.string.action_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showExportWarning = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun darkModeLabel(key: String): String {
    return when (key) {
        "light" -> stringResource(R.string.settings_dark_mode_light)
        "dark" -> stringResource(R.string.settings_dark_mode_dark)
        else -> stringResource(R.string.settings_dark_mode_system)
    }
}

@Composable
private fun languageLabel(key: String): String {
    return when (key) {
        "zh" -> "中文"
        "en" -> "English"
        else -> stringResource(R.string.settings_language_system)
    }
}
