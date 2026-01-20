package io.github.simplek.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import io.github.simplek.model.SimpleKId
import io.github.simplek.model.SimpleKItem

/**
 * Represents the zoom-out transition phase during drag.
 */
enum class ZoomPhase {
    /** Normal view - no zoom transition */
    Normal,
    /** Transitioning from normal to miniature view */
    ZoomingOut,
    /** Fully zoomed out - miniature column view */
    Miniature,
    /** Transitioning from miniature back to normal view */
    ZoomingIn,
}

/**
 * Represents the current drag operation state.
 */
@Immutable
sealed interface DragState<out T : SimpleKItem> {

    /**
     * No drag operation in progress.
     */
    data object Idle : DragState<Nothing>

    /**
     * A card is being dragged.
     */
    data class Dragging<T : SimpleKItem>(
        val item: T,
        val sourceColumnId: SimpleKId,
        val sourceIndex: Int,
        val currentOffset: Offset,
        val dragStartOffset: Offset,
        /** Where the user touched relative to the card's top-left corner */
        val touchOffsetInCard: Offset = Offset.Zero,
    ) : DragState<T>

    /**
     * A card is being dragged and is over a valid drop target.
     */
    data class OverDropTarget<T : SimpleKItem>(
        val item: T,
        val sourceColumnId: SimpleKId,
        val sourceIndex: Int,
        val targetColumnId: SimpleKId,
        val targetIndex: Int,
        val currentOffset: Offset,
        /** Where the user touched relative to the card's top-left corner */
        val touchOffsetInCard: Offset = Offset.Zero,
    ) : DragState<T>
}

/**
 * Represents the column drag state for reordering columns.
 */
@Immutable
sealed interface ColumnDragState {

    /**
     * No column drag in progress.
     */
    data object Idle : ColumnDragState

    /**
     * A column is being dragged.
     */
    data class Dragging(
        val columnId: SimpleKId,
        val sourceIndex: Int,
        val currentOffset: Offset,
    ) : ColumnDragState
}
