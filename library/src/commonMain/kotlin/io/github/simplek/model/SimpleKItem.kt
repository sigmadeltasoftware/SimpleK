package io.github.simplek.model

import androidx.compose.runtime.Stable

/**
 * Interface for items that can be placed in a Kanban board.
 * Implement this interface for custom card data types.
 *
 * Implementations should be stable (immutable or have stable identity)
 * to allow Compose to properly skip recomposition.
 */
@Stable
interface SimpleKItem {
    val id: SimpleKId
}
