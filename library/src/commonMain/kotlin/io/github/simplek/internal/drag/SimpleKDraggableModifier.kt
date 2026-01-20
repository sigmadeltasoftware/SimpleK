package io.github.simplek.internal.drag

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import io.github.simplek.internal.AutoScrollController
import io.github.simplek.internal.DropZoneRegistry
import io.github.simplek.model.SimpleKId
import io.github.simplek.model.SimpleKItem
import io.github.simplek.state.SimpleKState
import io.github.simplek.state.ZoomPhase
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Scope for kanban drag callbacks.
 * Provides context about the current drag operation.
 */
internal interface KanbanDragScope {
    /** Current drag position in root coordinates */
    val currentPosition: Offset
    /** Whether the card has left its source column */
    val hasLeftSourceColumn: Boolean
    /** Current column ID during drag */
    val currentColumnId: SimpleKId
    /** Current index within the column */
    val currentIndex: Int
}

/**
 * Callbacks for drag events during kanban card dragging.
 */
internal interface KanbanDragCallbacks<T : SimpleKItem> {
    /** Called when drag starts after long press */
    fun onDragStart(item: T, columnId: SimpleKId, index: Int, position: Offset, touchOffset: Offset)
    /** Called when drag position updates */
    fun onDragUpdate(position: Offset, targetColumnId: SimpleKId?, targetIndex: Int?)
    /** Called when card is moved to a new position */
    fun onCardMove(cardId: SimpleKId, targetColumnId: SimpleKId, targetIndex: Int)
    /** Called when drag ends */
    fun onDragEnd()
    /** Called to check if zoom should trigger */
    fun onCheckZoom(position: Offset, sourceColumnBounds: Rect?)
    /** Called to find drop target at position */
    fun findDropTarget(position: Offset, cardId: SimpleKId, fromColumnId: SimpleKId): DropTargetResult?
    /** Get card height for center calculation */
    fun getCardHeight(cardId: SimpleKId): Float?
}

/**
 * Creates callbacks that delegate to SimpleKState and related components.
 */
internal fun <T : SimpleKItem> createDragCallbacks(
    state: SimpleKState<T>,
    dropZoneRegistry: DropZoneRegistry,
    zoomController: ZoomTransitionController<T>,
    hapticFeedback: HapticFeedback,
): KanbanDragCallbacks<T> = object : KanbanDragCallbacks<T> {

    override fun onDragStart(item: T, columnId: SimpleKId, index: Int, position: Offset, touchOffset: Offset) {
        DragHaptics.onDragStart(hapticFeedback)
        state.startCardDrag(item, columnId, index, position, touchOffset)
    }

    override fun onDragUpdate(position: Offset, targetColumnId: SimpleKId?, targetIndex: Int?) {
        state.updateCardDrag(position, targetColumnId, targetIndex)
    }

    override fun onCardMove(cardId: SimpleKId, targetColumnId: SimpleKId, targetIndex: Int) {
        DragHaptics.onPositionChange(hapticFeedback)
        state.moveCard(cardId, targetColumnId, targetIndex)
    }

    override fun onDragEnd() {
        state.endCardDrag()
        zoomController.triggerZoomIn()
    }

    override fun onCheckZoom(position: Offset, sourceColumnBounds: Rect?) {
        zoomController.checkAndTriggerZoomOut(position, sourceColumnBounds)
    }

    override fun findDropTarget(position: Offset, cardId: SimpleKId, fromColumnId: SimpleKId): DropTargetResult? {
        val target = dropZoneRegistry.findDropTarget(position, cardId, fromColumnId)
        return target?.let { DropTargetResult(it.columnId, it.index) }
    }

    override fun getCardHeight(cardId: SimpleKId): Float? {
        return state.cardHeights[cardId]
    }
}

/**
 * A modifier that enables kanban-style drag behavior on a card.
 *
 * Features:
 * - Long press to initiate drag
 * - Immediate card movement during drag
 * - Zoom-out transition when leaving source column
 * - Debounced position updates to prevent jitter
 * - Haptic feedback for drag events
 *
 * @param item The item being dragged
 * @param columnId The column containing the item
 * @param index The item's position in the column
 * @param state The kanban state holder
 * @param dropZoneRegistry Registry for drop zone detection
 * @param autoScrollController Controller for auto-scroll during drag
 * @param zoomController Controller for zoom transitions
 * @param hapticFeedback Platform haptic feedback
 * @param config Drag configuration
 * @param cardPosition Current card position provider
 */
internal fun <T : SimpleKItem> Modifier.kanbanDraggable(
    item: T,
    columnId: SimpleKId,
    index: Int,
    state: SimpleKState<T>,
    dropZoneRegistry: DropZoneRegistry,
    autoScrollController: AutoScrollController,
    zoomController: ZoomTransitionController<T>,
    hapticFeedback: HapticFeedback,
    config: DragConfiguration,
    cardPosition: () -> Offset,
): Modifier {
    if (!config.enableCardDrag) {
        return this
    }

    return this.pointerInput(item.id, config.enableCardDrag) {
        val dragStateManager = CardDragStateManager()
        val callbacks = createDragCallbacks(state, dropZoneRegistry, zoomController, hapticFeedback)

        awaitEachGesture {
            // Wait for initial touch - don't consume yet to allow scroll
            val down = awaitFirstDown(requireUnconsumed = false)
            val startPosition = down.position
            val longPressTimeout = viewConfiguration.longPressTimeoutMillis

            // Try to detect long press without blocking scroll
            val longPressDetected = withTimeoutOrNull(longPressTimeout) {
                while (true) {
                    // Use Main pass to observe without blocking
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull { it.id == down.id }

                    if (change == null || !change.pressed) {
                        // Finger lifted - not a long press
                        return@withTimeoutOrNull false
                    }

                    // Check if moved too far - this is a scroll gesture
                    val distance = (change.position - startPosition).getDistance()
                    if (distance > viewConfiguration.touchSlop) {
                        // User is scrolling, don't intercept
                        return@withTimeoutOrNull false
                    }
                }
                @Suppress("UNREACHABLE_CODE")
                false
            } == null // timeout means long press succeeded

            if (longPressDetected) {
                // NOW we take over - disable scroll and consume events
                state.setPointerDownOnCard(true)

                try {
                    // Initialize drag state
                    val currentCardPosition = cardPosition()
                    val dragStartPosition = currentCardPosition + startPosition
                    val sourceColumnBounds = dropZoneRegistry.getColumnBounds(columnId)

                    dragStateManager.startDrag(
                        cardPosition = currentCardPosition,
                        touchOffset = startPosition,
                        sourceColumnBounds = sourceColumnBounds,
                        columnId = columnId,
                        index = index,
                    )

                    // Start the drag in state
                    callbacks.onDragStart(item, columnId, index, dragStartPosition, startPosition)

                    // Track drag movement
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id }

                        if (change == null || !change.pressed) {
                            break
                        }

                        change.consume()
                        val currentPosition = currentCardPosition + change.position
                        dragStateManager.updatePosition(currentPosition)

                        // Check if we should trigger zoom
                        callbacks.onCheckZoom(currentPosition, sourceColumnBounds)

                        // Handle movement based on zoom state
                        if (state.zoomPhase == ZoomPhase.Normal) {
                            // Normal mode - find drop target and move immediately
                            handleNormalDrag(
                                item = item,
                                currentPosition = currentPosition,
                                startPosition = startPosition,
                                dragStateManager = dragStateManager,
                                callbacks = callbacks,
                            )
                        } else {
                            // Zoomed mode - just update visual position
                            // Overlay handles drop target via hoveredOverlayColumnId
                            callbacks.onDragUpdate(currentPosition, null, null)
                        }
                    }

                    // Drag ended - cleanup
                    autoScrollController.stopAll()
                    callbacks.onDragEnd()

                } finally {
                    state.setPointerDownOnCard(false)
                    dragStateManager.reset()
                    // Safety: ensure zoom resets if drag ends unexpectedly
                    zoomController.ensureZoomReset()
                }
            }
            // If not long press, gesture ends and scroll can handle it
        }
    }
}

/**
 * Handle drag movement in normal (non-zoomed) mode.
 * Finds drop targets and moves card immediately with debouncing.
 */
private fun <T : SimpleKItem> handleNormalDrag(
    item: T,
    currentPosition: Offset,
    startPosition: Offset,
    dragStateManager: CardDragStateManager,
    callbacks: KanbanDragCallbacks<T>,
) {
    // Find drop target using card center
    val cardHeight = callbacks.getCardHeight(item.id) ?: 0f
    val cardTopY = currentPosition.y - startPosition.y
    val cardCenterY = cardTopY + cardHeight / 2f
    val cardCenterPosition = Offset(
        x = currentPosition.x,
        y = cardCenterY,
    )

    val currentColumnId = dragStateManager.currentColumnId ?: return
    val dropTarget = callbacks.findDropTarget(cardCenterPosition, item.id, currentColumnId)

    // Check if we should move the card
    val canMove = dragStateManager.canMove()

    if (canMove && dropTarget != null) {
        val targetColumnId = dropTarget.columnId
        val targetIndex = dropTarget.index

        // Only move if position actually changed
        if (dragStateManager.hasPositionChanged(targetColumnId, targetIndex)) {
            callbacks.onCardMove(item.id, targetColumnId, targetIndex)
            dragStateManager.recordMove(targetColumnId, targetIndex)
        }
    }

    // Update drag visual position
    callbacks.onDragUpdate(
        position = currentPosition,
        targetColumnId = dropTarget?.columnId,
        targetIndex = dropTarget?.index,
    )
}
