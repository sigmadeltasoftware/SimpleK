package io.github.simplek.scope

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import io.github.simplek.model.SimpleKColumn
import io.github.simplek.model.SimpleKItem

/**
 * Scope for column content.
 */
@Stable
interface SimpleKColumnScope<T : SimpleKItem> {
    /**
     * The column being rendered.
     */
    val column: SimpleKColumn<T>

    /**
     * Whether this column is currently a drag target.
     */
    val isDragTarget: Boolean

    /**
     * Whether this column is being dragged.
     */
    val isBeingDragged: Boolean

    /**
     * Number of items in the column.
     */
    val itemCount: Int

    /**
     * Scroll state for the column's lazy list.
     */
    val scrollState: LazyListState

    /**
     * Modifier for the column drag handle.
     */
    fun Modifier.columnDragHandle(): Modifier
}

/**
 * Internal implementation of [SimpleKColumnScope].
 */
internal class SimpleKColumnScopeImpl<T : SimpleKItem>(
    override val column: SimpleKColumn<T>,
    override val isDragTarget: Boolean,
    override val isBeingDragged: Boolean,
    override val scrollState: LazyListState,
    private val dragHandleModifier: Modifier,
) : SimpleKColumnScope<T> {
    override val itemCount: Int get() = column.items.size

    override fun Modifier.columnDragHandle(): Modifier = this.then(dragHandleModifier)
}
