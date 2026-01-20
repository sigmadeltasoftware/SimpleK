package io.github.kankan.demo

import android.app.Application
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.github.kankan.demo.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class KankanDemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Napier.base(DebugAntilog())

        startKoin {
            androidLogger()
            androidContext(this@KankanDemoApplication)
            modules(appModule)
        }
    }
}
