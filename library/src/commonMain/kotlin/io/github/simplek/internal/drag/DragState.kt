package io.github.simplek.internal.drag

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.simplek.model.SimpleKId
import kotlin.time.TimeSource

/**
 * Internal drag state manager for a single card.
 * Encapsulates all drag-related state including position tracking,
 * debouncing, and column exit detection.
 */
@Stable
internal class CardDragStateManager {

    /** Current card position in root coordinates */
    var cardPosition by mutableStateOf(Offset.Zero)
        private set

    /** Where the user initially touched relative to card's top-left */
    var touchOffset by mutableStateOf(Offset.Zero)
        private set

    /** Source column bounds for exit detection */
    var sourceColumnBounds: Rect? = null
        private set

    /** Current column where the card is located */
    var currentColumnId: SimpleKId? = null
        private set

    /** Current index within the column */
    var currentIndex: Int = 0
        private set

    /** Time source for move debouncing */
    private val timeSource = TimeSource.Monotonic

    /** Last time we moved the card (for debouncing) */
    private var lastMoveTime = timeSource.markNow()

    /** Minimum time between moves to prevent rapid oscillation */
    private val moveDebounceMs = 150L

    /**
     * Initialize the drag state when starting a drag operation.
     */
    fun startDrag(
        cardPosition: Offset,
        touchOffset: Offset,
        sourceColumnBounds: Rect?,
        columnId: SimpleKId,
        index: Int,
    ) {
        this.cardPosition = cardPosition
        this.touchOffset = touchOffset
        this.sourceColumnBounds = sourceColumnBounds
        this.currentColumnId = columnId
        this.currentIndex = index
        this.lastMoveTime = timeSource.markNow()
    }

    /**
     * Update the card position during drag.
     */
    fun updatePosition(newPosition: Offset) {
        this.cardPosition = newPosition
    }

    /**
     * Check if the card has left the source column (trigger for zoom).
     * Uses horizontal position to detect leaving the column.
     */
    fun hasLeftSourceColumn(currentPosition: Offset): Boolean {
        val bounds = sourceColumnBounds ?: return false
        return currentPosition.x < bounds.left || currentPosition.x > bounds.right
    }

    /**
     * Check if enough time has passed since the last move to allow another move.
     * This prevents rapid oscillation when hovering near slot boundaries.
     */
    fun canMove(): Boolean {
        return lastMoveTime.elapsedNow().inWholeMilliseconds >= moveDebounceMs
    }

    /**
     * Record that a move just happened.
     */
    fun recordMove(newColumnId: SimpleKId, newIndex: Int) {
        this.currentColumnId = newColumnId
        this.currentIndex = newIndex
        this.lastMoveTime = timeSource.markNow()
    }

    /**
     * Check if the position has actually changed.
     */
    fun hasPositionChanged(targetColumnId: SimpleKId, targetIndex: Int): Boolean {
        return targetColumnId != currentColumnId || targetIndex != currentIndex
    }

    /**
     * Reset the drag state after drag ends.
     */
    fun reset() {
        cardPosition = Offset.Zero
        touchOffset = Offset.Zero
        sourceColumnBounds = null
        currentColumnId = null
        currentIndex = 0
    }
}

/**
 * Represents the result of a drop target calculation.
 */
internal data class DropTargetResult(
    val columnId: SimpleKId,
    val index: Int,
)

/**
 * Configuration for drag behavior.
 */
internal data class DragConfiguration(
    /** Whether card drag is enabled */
    val enableCardDrag: Boolean,
    /** Whether zoom-out drag mode is enabled */
    val enableZoomOutDrag: Boolean,
    /** Duration of zoom-out animation in milliseconds */
    val zoomOutDurationMillis: Long,
    /** Duration of zoom-in animation in milliseconds */
    val zoomInDurationMillis: Long,
)
