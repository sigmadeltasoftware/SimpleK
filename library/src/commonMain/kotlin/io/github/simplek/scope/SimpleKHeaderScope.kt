package io.github.simplek.scope

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

/**
 * Scope for column header content.
 */
@Stable
interface SimpleKHeaderScope {
    /**
     * Number of items in the column.
     */
    val itemCount: Int

    /**
     * Whether the column is at its WIP limit.
     */
    val isAtLimit: Boolean

    /**
     * Maximum number of items allowed (null if no limit).
     */
    val maxItems: Int?

    /**
     * Whether the column is currently collapsed.
     */
    val isCollapsed: Boolean

    /**
     * Toggle the collapsed state of the column.
     */
    fun toggleCollapse()

    /**
     * Modifier for the column drag handle.
     * Apply to the header to make it the drag handle for column reordering.
     */
    fun Modifier.columnDragHandle(): Modifier
}

/**
 * Internal implementation of [SimpleKHeaderScope].
 */
internal class SimpleKHeaderScopeImpl(
    override val itemCount: Int,
    override val isAtLimit: Boolean,
    override val maxItems: Int?,
    override val isCollapsed: Boolean,
    private val dragHandleModifier: Modifier,
    private val onToggleCollapse: () -> Unit,
) : SimpleKHeaderScope {
    override fun toggleCollapse() = onToggleCollapse()
    override fun Modifier.columnDragHandle(): Modifier = this.then(dragHandleModifier)
}
