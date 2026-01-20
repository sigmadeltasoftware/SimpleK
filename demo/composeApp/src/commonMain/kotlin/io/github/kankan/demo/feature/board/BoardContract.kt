package io.github.kankan.demo.feature.board

import androidx.compose.runtime.Immutable
import io.github.simplek.config.SimpleKConfig
import io.github.kankan.demo.components.CardStyle
import io.github.simplek.model.DefaultCard
import io.github.simplek.model.SimpleKBoard

/**
 * MVI Contract for the Board screen.
 */
object BoardContract {

    @Immutable
    data class State(
        val board: SimpleKBoard<DefaultCard>? = null,
        val isLoading: Boolean = true,
        val isDarkTheme: Boolean = false,
        val cardStyle: CardStyle = CardStyle.DEFAULT,
        val config: SimpleKConfig = SimpleKConfig.Default.copy(enforceWipLimits = true),
        val showAddCardSheet: Boolean = false,
        val isSidebarVisible: Boolean = true,
    )

    sealed interface SideEffect {
        data class ShowMessage(val message: String) : SideEffect
    }
}
