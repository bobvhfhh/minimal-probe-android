package com.minprobe.app

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.minprobe.app.data.local.preferences.UserPreferences
import com.minprobe.app.data.local.preferences.UserPreferencesRepository
import com.minprobe.app.data.remote.UpdateCheckService
import com.minprobe.app.ui.screen.server.ServerListScreen
import com.minprobe.app.ui.theme.MiniProbeTheme
import com.minprobe.app.ui.theme.ThemeColor
import kotlinx.coroutines.flow.first
import org.koin.android.ext.android.inject

/**
 * 主入口。
 *
 * 安全加固（来自代码审查）：
 * - 本地加密保存密码和 session
 * - 生物识别锁 fail-closed：启用后若设备生物识别不可用，不再静默放行，
 *   而是显示锁定页（用户到系统设置录入后可重试）
 */
class MainActivity : AppCompatActivity() {

    private val prefsRepo: UserPreferencesRepository by inject()
    private val updateCheckService: UpdateCheckService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val prefs by prefsRepo.preferencesFlow.collectAsState(initial = UserPreferences())
            val darkTheme = when (prefs.darkModeKey) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            // 应用内语言
            LaunchedEffect(prefs.languageKey) {
                val locales = when (prefs.languageKey) {
                    "zh" -> LocaleListCompat.create(java.util.Locale.SIMPLIFIED_CHINESE)
                    "en" -> LocaleListCompat.create(java.util.Locale.ENGLISH)
                    else -> LocaleListCompat.getEmptyLocaleList()
                }
                AppCompatDelegate.setApplicationLocales(locales)
            }

            var initialScreens by remember { mutableStateOf<List<Screen>?>(null) }
            var authState by remember { mutableStateOf<AuthState>(AuthState.Checking) }

            LaunchedEffect(Unit) {
                initialScreens = listOf(ServerListScreen())
                val initPrefs = prefsRepo.preferencesFlow.first()
                if (!initPrefs.biometricEnabled) {
                    authState = AuthState.Unlocked
                } else {
                    requestBiometricAuth { success ->
                        authState = if (success) AuthState.Unlocked else AuthState.Locked
                    }
                }
                // 静默检查更新
                val versionCode = runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getPackageInfo(packageName, 0).versionCode
                    }
                }.getOrDefault(0)
                updateCheckService.checkForUpdateSilent(versionCode)
            }

            MiniProbeTheme(
                themeColor = ThemeColor.fromKey(prefs.themeColorKey),
                darkTheme = darkTheme
            ) {
                val currentDensity = LocalDensity.current
                val adjustedDensity = Density(
                    density = currentDensity.density,
                    fontScale = currentDensity.fontScale * prefs.fontScale
                )
                CompositionLocalProvider(LocalDensity provides adjustedDensity) {
                    when (val s = authState) {
                        is AuthState.Checking -> LoadingScreen()
                        is AuthState.Locked -> LockedScreen(
                            onRetry = {
                                authState = AuthState.Checking
                                requestBiometricAuth { success ->
                                    authState =
                                        if (success) AuthState.Unlocked else AuthState.Locked
                                }
                            }
                        )
                        is AuthState.Unlocked -> {
                            val screens = initialScreens
                            if (screens != null) {
                                Navigator(screens) { navigator ->
                                    SlideTransition(navigator)
                                }
                            } else {
                                LoadingScreen()
                            }
                        }
                    }
                }
            }
        }
    }

    private sealed interface AuthState {
        data object Checking : AuthState
        data object Locked : AuthState
        data object Unlocked : AuthState
    }

    private fun requestBiometricAuth(onResult: (Boolean) -> Unit) {
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            // 审查修复：fail-closed，不再静默放行
            onResult(false)
            return
        }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    onResult(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // 用户取消/按返回：关闭应用而非放行
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        finish()
                    } else {
                        onResult(false)
                    }
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(promptInfo)
    }
}

@Composable
private fun LoadingScreen() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LockedScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.biometric_locked_title),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.biometric_locked_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.action_retry))
        }
    }
}
