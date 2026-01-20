package io.github.kankan.demo

import androidx.compose.ui.window.ComposeUIViewController
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.github.kankan.demo.di.appModule
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController { App() }

fun initKoin() {
    Napier.base(DebugAntilog())
    startKoin {
        modules(appModule)
    }
}
