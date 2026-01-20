package io.github.simplek.internal

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.simplek.model.SimpleKId

/**
 * Registry for tracking drop zones.
 * Used internally to detect valid drop targets during drag operations.
 */
@Stable
internal class DropZoneRegistry {
    private val _cardZones = mutableStateMapOf<String, CardDropZone>()
    private val _columnZones = mutableStateMapOf<SimpleKId, ColumnDropZone>()

    val cardZones: Map<String, CardDropZone> get() = _cardZones
    val columnZones: Map<SimpleKId, ColumnDropZone> get() = _columnZones

    fun registerCard(
        cardId: SimpleKId,
        columnId: SimpleKId,
        index: Int,
        bounds: Rect,
    ) {
        val key = "${columnId.value}:${cardId.value}"
        _cardZones[key] = CardDropZone(
            cardId = cardId,
            columnId = columnId,
            index = index,
            bounds = bounds,
        )
    }

    fun unregisterCard(cardId: SimpleKId, columnId: SimpleKId) {
        val key = "${columnId.value}:${cardId.value}"
        _cardZones.remove(key)
    }

    fun registerColumn(columnId: SimpleKId, index: Int, bounds: Rect) {
        _columnZones[columnId] = ColumnDropZone(
            columnId = columnId,
            index = index,
            bounds = bounds,
        )
    }

    fun unregisterColumn(columnId: SimpleKId) {
        _columnZones.remove(columnId)
    }

    /**
     * Find the best drop target for the given position.
     * Returns the column ID and insertion index.
     *
     * Since cards move immediately during drag (via moveCard), we use current positions.
     * The dragged card is excluded from hit testing.
     */
    fun findDropTarget(
        position: Offset,
        draggedCardId: SimpleKId,
        draggedFromColumnId: SimpleKId,
    ): DropTarget? {
        // First, find which column we're over
        val targetColumn = _columnZones.values.find { it.bounds.contains(position) }
            ?: return null

        // Find all cards in this column except the dragged one, sorted by position
        val cardsInColumn = _cardZones.values
            .filter { it.columnId == targetColumn.columnId && it.cardId != draggedCardId }
            .sortedBy { it.bounds.top }

        if (cardsInColumn.isEmpty()) {
            // Empty column (or only the dragged card), insert at index 0
            return DropTarget(targetColumn.columnId, 0)
        }

        // Find which slot the position falls into by comparing against midpoints
        for ((idx, card) in cardsInColumn.withIndex()) {
            val cardCenter = card.bounds.top + card.bounds.height / 2

            if (position.y < cardCenter) {
                return DropTarget(targetColumn.columnId, idx)
            }
        }

        // Insert at end
        return DropTarget(targetColumn.columnId, cardsInColumn.size)
    }

    /**
     * Find the column at the given position.
     */
    fun findColumnAt(position: Offset): SimpleKId? {
        return _columnZones.values.find { it.bounds.contains(position) }?.columnId
    }

    /**
     * Get the bounds of a specific column.
     */
    fun getColumnBounds(columnId: SimpleKId): Rect? {
        return _columnZones[columnId]?.bounds
    }

    /**
     * Get the bounds of multiple columns.
     */
    fun getColumnBounds(columnIds: List<SimpleKId>): Map<SimpleKId, Rect> {
        return columnIds.mapNotNull { id ->
            _columnZones[id]?.let { id to it.bounds }
        }.toMap()
    }

    fun clear() {
        _cardZones.clear()
        _columnZones.clear()
    }
}

internal data class CardDropZone(
    val cardId: SimpleKId,
    val columnId: SimpleKId,
    val index: Int,
    val bounds: Rect,
)

internal data class ColumnDropZone(
    val columnId: SimpleKId,
    val index: Int,
    val bounds: Rect,
)

internal data class DropTarget(
    val columnId: SimpleKId,
    val index: Int,
)
