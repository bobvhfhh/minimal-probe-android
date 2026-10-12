package com.minprobe.app

import android.app.Application
import com.minprobe.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MiniProbeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            // 优化项：release 包不输出 Koin 内部日志
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@MiniProbeApplication)
            modules(appModule)
        }
    }
}
