package io.github.simplek.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.simplek.serialization.ColorSerializer
import kotlinx.serialization.Serializable

/**
 * Represents a column in the Kanban board.
 *
 * @param T The type of items in this column, must implement [SimpleKItem]
 * @property id Unique identifier for this column
 * @property title Display title for the column header
 * @property items List of items in this column
 * @property color Optional accent color for the column
 * @property maxItems Optional WIP (Work In Progress) limit
 */
@Serializable
@Immutable
data class SimpleKColumn<T : SimpleKItem>(
    val id: SimpleKId,
    val title: String,
    val items: List<T>,
    @Serializable(with = ColorSerializer::class)
    val color: Color? = null,
    val maxItems: Int? = null,
    val isCollapsed: Boolean = false,
) {
    val isAtLimit: Boolean
        get() = maxItems != null && items.size >= maxItems
}
