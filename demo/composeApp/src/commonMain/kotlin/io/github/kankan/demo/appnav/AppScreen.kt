package io.github.kankan.demo.appnav

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the demo app.
 */
sealed interface AppScreen {
    @Serializable
    data object Board : AppScreen
}
