package io.github.simplek.state

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.simplek.config.SimpleKConfig
import io.github.simplek.model.SimpleKBoard
import io.github.simplek.model.SimpleKColumn
import io.github.simplek.model.SimpleKId
import io.github.simplek.model.SimpleKItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Central state holder for the Kanban board.
 *
 * This class manages the complete state of a Kanban board including:
 * - Board data (columns and cards)
 * - Drag operation state (for both cards and columns)
 * - Zoom-out animation state
 * - Undo/redo history
 *
 * ## Thread Safety
 *
 * **Important:** All state operations must be called from the main/UI thread.
 * This is standard practice for Compose state management. The state uses Compose's
 * `mutableStateOf` internally which is not thread-safe for concurrent modifications.
 * Calling state methods from background threads may cause race conditions.
 *
 * ## State Management
 *
 * The state follows a unidirectional data flow pattern. All mutations go through
 * public methods which update the internal state and trigger recomposition.
 *
 * ## Undo/Redo Support
 *
 * Operations that modify the board (moveCard, addCard, removeCard, addColumn, removeColumn)
 * are tracked in an undo/redo stack. Use [canUndo], [canRedo], [undo], and [redo] to
 * manage history navigation.
 *
 * ## Usage
 *
 * Create state using [rememberSimpleKState]:
 *
 * ```kotlin
 * val state = rememberSimpleKState(initialBoard)
 *
 * // Read current board
 * val columns = state.board.columns
 *
 * // Modify board
 * state.moveCard(cardId, targetColumnId, targetIndex)
 * state.addCard(columnId, newCard)
 *
 * // Check drag state
 * if (state.isDragging.value) {
 *     // Show drag UI
 * }
 *
 * // Undo/redo
 * if (state.canUndo) state.undo()
 * if (state.canRedo) state.redo()
 * ```
 *
 * @param T The type of items in the board, must implement [SimpleKItem]
 * @property board The current board state (read-only, use mutation methods to modify)
 * @property cardDragState Current state of card drag operation
 * @property columnDragState Current state of column drag operation
 * @property isDragging Derived state indicating if any drag is in progress
 * @property zoomPhase Current phase of zoom-out animation
 * @property canUndo Whether there are actions to undo
 * @property canRedo Whether there are actions to redo
 *
 * @see rememberSimpleKState
 * @see SimpleKBoard
 */
@Stable
class SimpleKState<T : SimpleKItem> internal constructor(
    initialBoard: SimpleKBoard<T>,
    internal val coroutineScope: CoroutineScope,
    private val maxHistorySize: Int = SimpleKConfig.Default.maxHistorySize,
) {
    // Board data
    private var _board by mutableStateOf(initialBoard)
    val board: SimpleKBoard<T> get() = _board

    // Card drag state
    private var _cardDragState by mutableStateOf<DragState<T>>(DragState.Idle)
    val cardDragState: DragState<T> get() = _cardDragState

    // Column drag state
    private var _columnDragState by mutableStateOf<ColumnDragState>(ColumnDragState.Idle)
    val columnDragState: ColumnDragState get() = _columnDragState

    // Track when pointer is down on a card (before long press completes)
    // This is used to disable scroll immediately on touch
    private var _isPointerDownOnCard by mutableStateOf(false)
    val isPointerDownOnCard: Boolean get() = _isPointerDownOnCard

    // Track which mini column is being hovered in the column selector overlay
    private var _hoveredOverlayColumnId by mutableStateOf<SimpleKId?>(null)
    val hoveredOverlayColumnId: SimpleKId? get() = _hoveredOverlayColumnId

    // Derived state for convenience
    val isDragging: State<Boolean> = derivedStateOf {
        _cardDragState !is DragState.Idle || _columnDragState !is ColumnDragState.Idle
    }

    // Combined state: should scroll be disabled?
    val shouldDisableScroll: State<Boolean> = derivedStateOf {
        _isPointerDownOnCard || _cardDragState !is DragState.Idle || _columnDragState !is ColumnDragState.Idle
    }

    // Internal state for drop zone tracking
    internal val dropZones = mutableStateMapOf<String, DropZoneInfo>()
    internal val columnBounds = mutableStateMapOf<SimpleKId, Rect>()
    internal val columnScrollStates = mutableStateMapOf<SimpleKId, LazyListState>()
    internal val cardHeights = mutableStateMapOf<SimpleKId, Float>()

    // Zoom-out animation state
    private var _zoomPhase by mutableStateOf(ZoomPhase.Normal)
    val zoomPhase: ZoomPhase get() = _zoomPhase

    private val _zoomProgress = Animatable(0f)
    val zoomProgress: Float get() = _zoomProgress.value

    // Track original card bounds for top-left corner animations
    internal val cardOriginalBounds = mutableStateMapOf<SimpleKId, Rect>()

    // Track current zoom animation job for cancellation
    private var zoomAnimationJob: Job? = null

    // ==================== Undo/Redo History ====================
    // Note: These stacks are accessed only from the main thread (see class documentation)

    private val undoStack = mutableListOf<SimpleKCommand<T>>()
    private val redoStack = mutableListOf<SimpleKCommand<T>>()

    // Track undo stack size for reactive UI updates
    private var _undoStackSize by mutableStateOf(0)
    private var _redoStackSize by mutableStateOf(0)

    /**
     * Whether there are commands that can be undone.
     */
    val canUndo: State<Boolean> = derivedStateOf { _undoStackSize > 0 }

    /**
     * Whether there are commands that can be redone.
     */
    val canRedo: State<Boolean> = derivedStateOf { _redoStackSize > 0 }

    /**
     * Records a command to the undo stack and clears the redo stack.
     * Enforces the maximum history size.
     */
    private fun recordCommand(command: SimpleKCommand<T>) {
        undoStack.add(command)
        redoStack.clear()

        // Enforce max history size
        while (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }

        _undoStackSize = undoStack.size
        _redoStackSize = 0
    }

    /**
     * Undo the last command.
     * Does nothing if there are no commands to undo.
     */
    fun undo() {
        if (undoStack.isEmpty()) return

        val command = undoStack.removeLast()
        executeCommandInverse(command)
        redoStack.add(command)

        _undoStackSize = undoStack.size
        _redoStackSize = redoStack.size
    }

    /**
     * Redo the last undone command.
     * Does nothing if there are no commands to redo.
     */
    fun redo() {
        if (redoStack.isEmpty()) return

        val command = redoStack.removeLast()
        executeCommand(command)
        undoStack.add(command)

        _undoStackSize = undoStack.size
        _redoStackSize = redoStack.size
    }

    /**
     * Clear both undo and redo history.
     */
    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
        _undoStackSize = 0
        _redoStackSize = 0
    }

    /**
     * Execute a command (for redo operations).
     */
    private fun executeCommand(command: SimpleKCommand<T>) {
        when (command) {
            is SimpleKCommand.MoveCard -> {
                moveCardInternal(command.cardId, command.toColumnId, command.toIndex)
            }
            is SimpleKCommand.AddCard -> {
                addCardInternal(command.columnId, command.card, command.index)
            }
            is SimpleKCommand.RemoveCard -> {
                removeCardInternal(command.columnId, command.cardId)
            }
            is SimpleKCommand.AddColumn -> {
                addColumnInternal(command.column, command.index)
            }
            is SimpleKCommand.RemoveColumn -> {
                removeColumnInternal(command.columnId)
            }
        }
    }

    /**
     * Execute the inverse of a command (for undo operations).
     */
    private fun executeCommandInverse(command: SimpleKCommand<T>) {
        when (command) {
            is SimpleKCommand.MoveCard -> {
                val inverse = command.inverse()
                moveCardInternal(inverse.cardId, inverse.toColumnId, inverse.toIndex)
            }
            is SimpleKCommand.AddCard -> {
                val inverse = command.inverse()
                removeCardInternal(inverse.columnId, inverse.cardId)
            }
            is SimpleKCommand.RemoveCard -> {
                val inverse = command.inverse()
                addCardInternal(inverse.columnId, inverse.card, inverse.index)
            }
            is SimpleKCommand.AddColumn -> {
                val inverse = command.inverse()
                removeColumnInternal(inverse.columnId)
            }
            is SimpleKCommand.RemoveColumn -> {
                val inverse = command.inverse()
                addColumnInternal(inverse.column, inverse.index)
            }
        }
    }

    // ==================== Public Actions ====================

    /**
     * Move a card to a new position.
     * Records a command for undo/redo support.
     */
    fun moveCard(
        cardId: SimpleKId,
        toColumnId: SimpleKId,
        toIndex: Int,
    ) {
        // Find the card's current position for the command
        var fromColumnId: SimpleKId? = null
        var fromIndex = -1

        for (column in _board.columns) {
            val cardIndex = column.items.indexOfFirst { it.id == cardId }
            if (cardIndex >= 0) {
                fromColumnId = column.id
                fromIndex = cardIndex
                break
            }
        }

        if (fromColumnId == null || fromIndex < 0) return

        // Skip if no actual move
        if (fromColumnId == toColumnId && fromIndex == toIndex) return

        // Record the command before executing
        recordCommand(
            SimpleKCommand.MoveCard(
                cardId = cardId,
                fromColumnId = fromColumnId,
                toColumnId = toColumnId,
                fromIndex = fromIndex,
                toIndex = toIndex,
            )
        )

        moveCardInternal(cardId, toColumnId, toIndex)
    }

    /**
     * Internal move card implementation without recording command.
     * Used by undo/redo operations.
     */
    private fun moveCardInternal(
        cardId: SimpleKId,
        toColumnId: SimpleKId,
        toIndex: Int,
    ) {
        val currentBoard = _board
        var movedCard: T? = null
        var fromColumnId: SimpleKId? = null

        // Find and remove the card from its current position
        val updatedColumns = currentBoard.columns.map { column ->
            val cardIndex = column.items.indexOfFirst { it.id == cardId }
            if (cardIndex >= 0) {
                movedCard = column.items[cardIndex]
                fromColumnId = column.id
                column.copy(items = column.items.toMutableList().apply { removeAt(cardIndex) })
            } else {
                column
            }
        }

        // Insert the card at the new position
        val finalColumns = updatedColumns.map { column ->
            if (column.id == toColumnId && movedCard != null) {
                val newItems = column.items.toMutableList()
                val adjustedIndex = if (fromColumnId == toColumnId && toIndex > 0) {
                    // Adjust index if moving within the same column
                    minOf(toIndex, newItems.size)
                } else {
                    minOf(toIndex, newItems.size)
                }
                newItems.add(adjustedIndex, movedCard!!)
                column.copy(items = newItems)
            } else {
                column
            }
        }

        _board = currentBoard.copy(columns = finalColumns)
    }

    /**
     * Move a column to a new position.
     * Note: Column moves are not currently tracked in undo/redo history.
     */
    fun moveColumn(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return

        val currentBoard = _board
        val columns = currentBoard.columns.toMutableList()
        val column = columns.removeAt(fromIndex)
        columns.add(toIndex, column)

        _board = currentBoard.copy(columns = columns)
    }

    /**
     * Add a card to a column.
     * Records a command for undo/redo support.
     */
    fun addCard(columnId: SimpleKId, card: T, index: Int? = null) {
        val column = _board.columns.find { it.id == columnId } ?: return
        val insertIndex = index ?: column.items.size
        val actualIndex = insertIndex.coerceIn(0, column.items.size)

        // Record the command before executing
        recordCommand(
            SimpleKCommand.AddCard(
                columnId = columnId,
                card = card,
                index = actualIndex,
            )
        )

        addCardInternal(columnId, card, actualIndex)
    }

    /**
     * Internal add card implementation without recording command.
     * Used by undo/redo operations.
     */
    private fun addCardInternal(columnId: SimpleKId, card: T, index: Int) {
        val currentBoard = _board
        val updatedColumns = currentBoard.columns.map { column ->
            if (column.id == columnId) {
                val newItems = column.items.toMutableList()
                newItems.add(index.coerceIn(0, newItems.size), card)
                column.copy(items = newItems)
            } else {
                column
            }
        }
        _board = currentBoard.copy(columns = updatedColumns)
    }

    /**
     * Remove a card from a column.
     * Records a command for undo/redo support.
     */
    fun removeCard(columnId: SimpleKId, cardId: SimpleKId) {
        val column = _board.columns.find { it.id == columnId } ?: return
        val cardIndex = column.items.indexOfFirst { it.id == cardId }
        if (cardIndex < 0) return

        val card = column.items[cardIndex]

        // Record the command before executing
        recordCommand(
            SimpleKCommand.RemoveCard(
                columnId = columnId,
                cardId = cardId,
                card = card,
                index = cardIndex,
            )
        )

        removeCardInternal(columnId, cardId)
    }

    /**
     * Internal remove card implementation without recording command.
     * Used by undo/redo operations.
     */
    private fun removeCardInternal(columnId: SimpleKId, cardId: SimpleKId) {
        val currentBoard = _board
        val updatedColumns = currentBoard.columns.map { column ->
            if (column.id == columnId) {
                column.copy(items = column.items.filter { it.id != cardId })
            } else {
                column
            }
        }
        _board = currentBoard.copy(columns = updatedColumns)
    }

    /**
     * Add a new column to the board.
     * Records a command for undo/redo support.
     */
    fun addColumn(column: SimpleKColumn<T>, index: Int? = null) {
        val columns = _board.columns
        val insertIndex = index ?: columns.size
        val actualIndex = insertIndex.coerceIn(0, columns.size)

        // Record the command before executing
        recordCommand(
            SimpleKCommand.AddColumn(
                column = column,
                index = actualIndex,
            )
        )

        addColumnInternal(column, actualIndex)
    }

    /**
     * Internal add column implementation without recording command.
     * Used by undo/redo operations.
     */
    private fun addColumnInternal(column: SimpleKColumn<T>, index: Int) {
        val currentBoard = _board
        val columns = currentBoard.columns.toMutableList()
        columns.add(index.coerceIn(0, columns.size), column)
        _board = currentBoard.copy(columns = columns)
    }

    /**
     * Remove a column from the board.
     * Records a command for undo/redo support.
     */
    fun removeColumn(columnId: SimpleKId) {
        val columnIndex = _board.columns.indexOfFirst { it.id == columnId }
        if (columnIndex < 0) return

        val column = _board.columns[columnIndex]

        // Record the command before executing
        recordCommand(
            SimpleKCommand.RemoveColumn(
                columnId = columnId,
                column = column,
                index = columnIndex,
            )
        )

        removeColumnInternal(columnId)
    }

    /**
     * Internal remove column implementation without recording command.
     * Used by undo/redo operations.
     */
    private fun removeColumnInternal(columnId: SimpleKId) {
        val currentBoard = _board
        _board = currentBoard.copy(columns = currentBoard.columns.filter { it.id != columnId })
    }

    /**
     * Update the entire board.
     * Note: This clears undo/redo history as it's a complete board replacement.
     */
    fun updateBoard(board: SimpleKBoard<T>) {
        _board = board
        clearHistory()
    }

    /**
     * Toggle the collapsed state of a column.
     */
    fun toggleColumnCollapsed(columnId: SimpleKId) {
        val currentBoard = _board
        val updatedColumns = currentBoard.columns.map { column ->
            if (column.id == columnId) {
                column.copy(isCollapsed = !column.isCollapsed)
            } else {
                column
            }
        }
        _board = currentBoard.copy(columns = updatedColumns)
    }

    /**
     * Set the collapsed state of a column.
     */
    fun setColumnCollapsed(columnId: SimpleKId, collapsed: Boolean) {
        val currentBoard = _board
        val updatedColumns = currentBoard.columns.map { column ->
            if (column.id == columnId) {
                column.copy(isCollapsed = collapsed)
            } else {
                column
            }
        }
        _board = currentBoard.copy(columns = updatedColumns)
    }

    // ==================== Card/Column Query & Update Methods ====================

    /**
     * Find a card by its ID.
     * Returns null if not found.
     */
    fun findCard(cardId: SimpleKId): T? {
        for (column in _board.columns) {
            val card = column.items.find { it.id == cardId }
            if (card != null) return card
        }
        return null
    }

    /**
     * Update a card using an updater function.
     * The updater receives the current card and should return the updated card.
     * Does nothing if the card is not found.
     *
     * Note: Card updates are not currently tracked in undo/redo history.
     *
     * @param cardId The ID of the card to update
     * @param updater A function that takes the current card and returns the updated card
     */
    fun updateCard(cardId: SimpleKId, updater: (T) -> T) {
        val currentBoard = _board
        var found = false

        val updatedColumns = currentBoard.columns.map { column ->
            val cardIndex = column.items.indexOfFirst { it.id == cardId }
            if (cardIndex >= 0) {
                found = true
                val updatedItems = column.items.toMutableList()
                updatedItems[cardIndex] = updater(updatedItems[cardIndex])
                column.copy(items = updatedItems)
            } else {
                column
            }
        }

        if (found) {
            _board = currentBoard.copy(columns = updatedColumns)
        }
    }

    /**
     * Update a column using an updater function.
     * The updater receives the current column and should return the updated column.
     * Does nothing if the column is not found.
     *
     * Note: Column updates are not currently tracked in undo/redo history.
     *
     * @param columnId The ID of the column to update
     * @param updater A function that takes the current column and returns the updated column
     */
    fun updateColumn(columnId: SimpleKId, updater: (SimpleKColumn<T>) -> SimpleKColumn<T>) {
        val currentBoard = _board
        val columnIndex = currentBoard.columns.indexOfFirst { it.id == columnId }

        if (columnIndex >= 0) {
            val updatedColumns = currentBoard.columns.toMutableList()
            updatedColumns[columnIndex] = updater(updatedColumns[columnIndex])
            _board = currentBoard.copy(columns = updatedColumns)
        }
    }

    // ==================== Internal Drag Operations ====================

    internal fun setPointerDownOnCard(down: Boolean) {
        _isPointerDownOnCard = down
    }

    internal fun setHoveredOverlayColumn(columnId: SimpleKId?) {
        _hoveredOverlayColumnId = columnId
    }

    internal fun clearHoveredOverlayColumn() {
        _hoveredOverlayColumnId = null
    }

    internal fun startCardDrag(item: T, columnId: SimpleKId, index: Int, offset: Offset, touchOffsetInCard: Offset) {
        _cardDragState = DragState.Dragging(
            item = item,
            sourceColumnId = columnId,
            sourceIndex = index,
            currentOffset = offset,
            dragStartOffset = offset,
            touchOffsetInCard = touchOffsetInCard,
        )
    }

    internal fun updateCardDrag(offset: Offset, targetColumnId: SimpleKId? = null, targetIndex: Int? = null) {
        when (val current = _cardDragState) {
            is DragState.Dragging -> {
                _cardDragState = if (targetColumnId != null && targetIndex != null) {
                    DragState.OverDropTarget(
                        item = current.item,
                        sourceColumnId = current.sourceColumnId,
                        sourceIndex = current.sourceIndex,
                        targetColumnId = targetColumnId,
                        targetIndex = targetIndex,
                        currentOffset = offset,
                        touchOffsetInCard = current.touchOffsetInCard,
                    )
                } else {
                    current.copy(currentOffset = offset)
                }
            }
            is DragState.OverDropTarget -> {
                _cardDragState = if (targetColumnId != null && targetIndex != null) {
                    current.copy(
                        targetColumnId = targetColumnId,
                        targetIndex = targetIndex,
                        currentOffset = offset,
                    )
                } else {
                    DragState.Dragging(
                        item = current.item,
                        sourceColumnId = current.sourceColumnId,
                        sourceIndex = current.sourceIndex,
                        currentOffset = offset,
                        dragStartOffset = offset,
                        touchOffsetInCard = current.touchOffsetInCard,
                    )
                }
            }
            DragState.Idle -> { /* no-op */ }
        }
    }

    internal fun endCardDrag(canMoveCard: ((cardId: SimpleKId, toColumnId: SimpleKId) -> Boolean)? = null): Boolean {
        val state = _cardDragState
        _cardDragState = DragState.Idle

        // Check if we should drop to the hovered overlay column instead
        val overlayTarget = _hoveredOverlayColumnId
        _hoveredOverlayColumnId = null

        // If hovering over overlay column, drop there
        if (overlayTarget != null) {
            val item = when (state) {
                is DragState.Dragging -> state.item
                is DragState.OverDropTarget -> state.item
                else -> return false
            }
            // Check if move is allowed by validation callback
            val moveAllowed = canMoveCard?.invoke(item.id, overlayTarget) != false
            if (!moveAllowed) {
                return false
            }
            val targetColumn = _board.columns.find { it.id == overlayTarget }
            if (targetColumn != null) {
                moveCard(item.id, overlayTarget, targetColumn.items.size)
                return true
            }
        }

        // Card is already in position - moves happen immediately during drag
        // Just return true if we were in a drag state
        return state !is DragState.Idle
    }

    internal fun cancelCardDrag() {
        _cardDragState = DragState.Idle
        _hoveredOverlayColumnId = null
    }

    /**
     * Drop the currently dragged card to a specific column (via overlay).
     * The card is added to the end of the target column.
     *
     * @param targetColumnId The ID of the column to drop the card into
     * @param canMoveCard Optional validation callback to check if the move is allowed
     */
    internal fun dropCardToColumn(
        targetColumnId: SimpleKId,
        canMoveCard: ((cardId: SimpleKId, toColumnId: SimpleKId) -> Boolean)? = null,
    ): Boolean {
        val state = _cardDragState
        _cardDragState = DragState.Idle
        _hoveredOverlayColumnId = null

        val item = when (state) {
            is DragState.Dragging -> state.item
            is DragState.OverDropTarget -> state.item
            else -> return false
        }

        // Check if move is allowed by validation callback
        val moveAllowed = canMoveCard?.invoke(item.id, targetColumnId) != false
        if (!moveAllowed) {
            return false
        }

        // Find the target column and add to the end
        val targetColumn = _board.columns.find { it.id == targetColumnId }
        if (targetColumn != null) {
            moveCard(item.id, targetColumnId, targetColumn.items.size)
            return true
        }
        return false
    }

    internal fun startColumnDrag(columnId: SimpleKId, index: Int, offset: Offset) {
        _columnDragState = ColumnDragState.Dragging(
            columnId = columnId,
            sourceIndex = index,
            currentOffset = offset,
        )
    }

    internal fun updateColumnDrag(offset: Offset) {
        val current = _columnDragState
        if (current is ColumnDragState.Dragging) {
            _columnDragState = current.copy(currentOffset = offset)
        }
    }

    internal fun endColumnDrag() {
        _columnDragState = ColumnDragState.Idle
    }

    // ==================== Zoom-Out Drag Operations ====================

    /**
     * Capture current card bounds for top-left corner scaling animation.
     * Call this before starting the zoom-out transition.
     */
    internal fun captureCardBounds(bounds: Map<SimpleKId, Rect>) {
        cardOriginalBounds.clear()
        cardOriginalBounds.putAll(bounds)
    }

    /**
     * Start the zoom-out transition animation.
     * This animates zoomProgress from 0 to 1 over the specified duration.
     *
     * @param durationMillis The duration of the zoom-out animation
     */
    internal fun startZoomOutTransition(durationMillis: Long = 800L) {
        zoomAnimationJob?.cancel()
        // Set phase immediately (not inside coroutine) to avoid race conditions
        _zoomPhase = ZoomPhase.ZoomingOut
        zoomAnimationJob = coroutineScope.launch {
            _zoomProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = durationMillis.toInt(),
                    easing = FastOutSlowInEasing
                )
            )
            _zoomPhase = ZoomPhase.Miniature
        }
    }

    /**
     * Start the zoom-in transition animation.
     * This animates zoomProgress from 1 to 0 over the specified duration.
     *
     * @param durationMillis The duration of the zoom-in animation
     */
    internal fun startZoomInTransition(durationMillis: Long = 400L) {
        zoomAnimationJob?.cancel()
        // Set phase immediately (not inside coroutine) to avoid race conditions
        _zoomPhase = ZoomPhase.ZoomingIn
        zoomAnimationJob = coroutineScope.launch {
            _zoomProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = durationMillis.toInt(),
                    easing = FastOutSlowInEasing
                )
            )
            _zoomPhase = ZoomPhase.Normal
            cardOriginalBounds.clear()
        }
    }

    /**
     * Cancel any ongoing zoom animation and reset to normal state.
     */
    internal fun cancelZoomTransition() {
        zoomAnimationJob?.cancel()
        zoomAnimationJob = null
        coroutineScope.launch {
            _zoomProgress.snapTo(0f)
        }
        _zoomPhase = ZoomPhase.Normal
        cardOriginalBounds.clear()
    }

    /**
     * Check if we're currently in a zoom transition or zoomed out state.
     */
    val isZoomedOrTransitioning: Boolean
        get() = _zoomPhase != ZoomPhase.Normal
}

/**
 * Information about a drop zone.
 */
internal data class DropZoneInfo(
    val columnId: SimpleKId,
    val index: Int,
    val bounds: Rect,
)

/**
 * Creates and remembers a [SimpleKState] instance.
 *
 * @param initialBoard The initial board configuration
 * @param maxHistorySize Maximum number of commands to keep in undo history (default: 50)
 */
@Composable
fun <T : SimpleKItem> rememberSimpleKState(
    initialBoard: SimpleKBoard<T>,
    maxHistorySize: Int = SimpleKConfig.Default.maxHistorySize,
): SimpleKState<T> {
    val coroutineScope = rememberCoroutineScope()
    return remember(initialBoard.id, maxHistorySize) {
        SimpleKState(initialBoard, coroutineScope, maxHistorySize)
    }
}
