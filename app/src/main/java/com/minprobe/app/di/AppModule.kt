package com.minprobe.app.di

import android.util.Log
import androidx.room.Room
import com.minprobe.app.BuildConfig
import com.minprobe.app.data.backup.ConfigBackupManager
import com.minprobe.app.data.local.MiniProbeDatabase
import com.minprobe.app.data.local.crypto.CryptoManager
import com.minprobe.app.data.local.preferences.UserPreferencesRepository
import com.minprobe.app.data.remote.MonitorAuthService
import com.minprobe.app.data.remote.MonitorNodeService
import com.minprobe.app.data.remote.MonitorWebSocket
import com.minprobe.app.data.remote.SessionCookieInterceptor
import com.minprobe.app.data.remote.SessionManager
import com.minprobe.app.data.remote.UpdateCheckService
import com.minprobe.app.data.repository.NodeRepositoryImpl
import com.minprobe.app.data.repository.ServerRepositoryImpl
import com.minprobe.app.domain.repository.NodeRepository
import com.minprobe.app.domain.repository.ServerRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { CryptoManager() }
    single { SessionManager() }
    single { UserPreferencesRepository(androidContext()) }

    single {
        Room.databaseBuilder(
            androidContext(),
            MiniProbeDatabase::class.java,
            "minprobe_database"
        ).build()
    }
    single { get<MiniProbeDatabase>().serverInstanceDao() }

    single {
        val sessionManager: SessionManager = get()
        HttpClient(OkHttp) {
            engine {
                // 注入 session cookie（带 host 校验，见 SessionCookieInterceptor）
                addInterceptor(SessionCookieInterceptor(sessionManager))
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
            install(WebSockets.Plugin)
            install(Logging) {
                // 优化项：release 包不记录网络日志，避免密码/cookie 进 logcat
                level = if (BuildConfig.DEBUG) LogLevel.HEADERS else LogLevel.NONE
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("KtorHttp", message)
                    }
                }
                // 即使 debug 也不记录 body：登录请求含明文密码
                sanitizeHeader { header -> header == "Cookie" || header == "Set-Cookie" }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
        }
    }

    single { MonitorAuthService(get()) }
    single { MonitorNodeService(get()) }
    single { MonitorWebSocket(get()) }
    single {
        // 更新检查用独立 client：不带 session 拦截器
        val updateClient = HttpClient(OkHttp) {
            install(HttpTimeout) { requestTimeoutMillis = 15_000 }
        }
        UpdateCheckService(updateClient)
    }

    single<ServerRepository> { ServerRepositoryImpl(get(), get(), get(), get()) }
    single<NodeRepository> { NodeRepositoryImpl(get(), get()) }

    single {
        ConfigBackupManager(
            androidContext(),
            get(),
            Json { prettyPrint = true; ignoreUnknownKeys = true }
        )
    }

    // ScreenModels
    factory { com.minprobe.app.ui.screen.server.ServerViewModel(get(), get()) }
    factory { (nodeId: Long) ->
        com.minprobe.app.ui.screen.nodedetail.NodeDetailViewModel(nodeId, get(), get(), get())
    }
    factory { com.minprobe.app.ui.screen.nodelist.NodeListViewModel(get(), get(), get()) }
    factory { com.minprobe.app.ui.screen.ping.PingMonitorViewModel(get(), get(), get()) }
    factory { com.minprobe.app.ui.screen.settings.SettingsViewModel(get(), get(), get()) }
    factory { com.minprobe.app.ui.screen.settings.AboutViewModel(get(), get()) }
    factory { com.minprobe.app.ui.screen.server.ServerReLoginViewModel(get(), get()) }
}
