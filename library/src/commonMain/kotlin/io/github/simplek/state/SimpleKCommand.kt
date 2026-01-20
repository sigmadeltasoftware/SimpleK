package io.github.simplek.state

import io.github.simplek.model.SimpleKColumn
import io.github.simplek.model.SimpleKId
import io.github.simplek.model.SimpleKItem

/**
 * Represents an undoable/redoable command on the Kanban board.
 * All commands are immutable and can be reversed.
 *
 * @param T The type of items in the board
 */
sealed class SimpleKCommand<out T : SimpleKItem> {

    /**
     * Command representing moving a card from one position to another.
     *
     * @property cardId The ID of the card being moved
     * @property fromColumnId The source column ID
     * @property toColumnId The destination column ID
     * @property fromIndex The original index within the source column
     * @property toIndex The target index within the destination column
     */
    data class MoveCard(
        val cardId: SimpleKId,
        val fromColumnId: SimpleKId,
        val toColumnId: SimpleKId,
        val fromIndex: Int,
        val toIndex: Int,
    ) : SimpleKCommand<Nothing>() {
        /**
         * Creates the inverse command that undoes this move.
         */
        fun inverse(): MoveCard = MoveCard(
            cardId = cardId,
            fromColumnId = toColumnId,
            toColumnId = fromColumnId,
            fromIndex = toIndex,
            toIndex = fromIndex,
        )
    }

    /**
     * Command representing adding a card to a column.
     *
     * @property columnId The ID of the column to add the card to
     * @property card The card being added
     * @property index The index where the card was inserted
     */
    data class AddCard<T : SimpleKItem>(
        val columnId: SimpleKId,
        val card: T,
        val index: Int,
    ) : SimpleKCommand<T>() {
        /**
         * Creates the inverse command that removes the added card.
         */
        fun inverse(): RemoveCard<T> = RemoveCard(
            columnId = columnId,
            cardId = card.id,
            card = card,
            index = index,
        )
    }

    /**
     * Command representing removing a card from a column.
     *
     * @property columnId The ID of the column the card was removed from
     * @property cardId The ID of the removed card
     * @property card The card that was removed (stored for undo)
     * @property index The index where the card was located before removal
     */
    data class RemoveCard<T : SimpleKItem>(
        val columnId: SimpleKId,
        val cardId: SimpleKId,
        val card: T,
        val index: Int,
    ) : SimpleKCommand<T>() {
        /**
         * Creates the inverse command that re-adds the removed card.
         */
        fun inverse(): AddCard<T> = AddCard(
            columnId = columnId,
            card = card,
            index = index,
        )
    }

    /**
     * Command representing adding a column to the board.
     *
     * @property column The column being added
     * @property index The index where the column was inserted
     */
    data class AddColumn<T : SimpleKItem>(
        val column: SimpleKColumn<T>,
        val index: Int,
    ) : SimpleKCommand<T>() {
        /**
         * Creates the inverse command that removes the added column.
         */
        fun inverse(): RemoveColumn<T> = RemoveColumn(
            columnId = column.id,
            column = column,
            index = index,
        )
    }

    /**
     * Command representing removing a column from the board.
     *
     * @property columnId The ID of the removed column
     * @property column The column that was removed (stored for undo)
     * @property index The index where the column was located before removal
     */
    data class RemoveColumn<T : SimpleKItem>(
        val columnId: SimpleKId,
        val column: SimpleKColumn<T>,
        val index: Int,
    ) : SimpleKCommand<T>() {
        /**
         * Creates the inverse command that re-adds the removed column.
         */
        fun inverse(): AddColumn<T> = AddColumn(
            column = column,
            index = index,
        )
    }
}
