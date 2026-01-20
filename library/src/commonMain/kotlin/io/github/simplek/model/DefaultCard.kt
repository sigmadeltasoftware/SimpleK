package io.github.simplek.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.simplek.serialization.ColorSerializer
import kotlinx.serialization.Serializable

/**
 * Default card implementation for quick prototyping.
 * Use this or create your own implementation of [SimpleKItem].
 */
@Serializable
@Immutable
data class DefaultCard(
    override val id: SimpleKId = SimpleKId.generate(),
    val title: String,
    val description: String? = null,
    val labels: List<CardLabel> = emptyList(),
    val priority: CardPriority = CardPriority.NONE,
    val imageUrl: String? = null,
) : SimpleKItem

/**
 * Label that can be attached to cards.
 */
@Serializable
@Immutable
data class CardLabel(
    val id: SimpleKId = SimpleKId.generate(),
    val name: String,
    @Serializable(with = ColorSerializer::class)
    val color: Color,
)

/**
 * Priority levels for cards.
 */
@Serializable
enum class CardPriority {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    URGENT,
}
