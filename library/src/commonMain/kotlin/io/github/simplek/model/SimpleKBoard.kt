package io.github.simplek.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Represents the entire Kanban board.
 *
 * @param T The type of items in the board, must implement [SimpleKItem]
 * @property id Unique identifier for this board
 * @property columns List of columns in display order
 */
@Serializable
@Immutable
data class SimpleKBoard<T : SimpleKItem>(
    val id: SimpleKId,
    val columns: List<SimpleKColumn<T>>,
)
