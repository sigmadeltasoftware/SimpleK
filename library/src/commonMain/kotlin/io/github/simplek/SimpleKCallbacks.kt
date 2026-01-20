package io.github.simplek

import io.github.simplek.model.SimpleKId
import io.github.simplek.model.SimpleKItem

/**
 * Callback handlers for Kanban board events.
 *
 * Group all event callbacks into a single object for cleaner API usage.
 *
 * ## Usage
 *
 * ```kotlin
 * SimpleKBoard(
 *     state = state,
 *     callbacks = SimpleKCallbacks(
 *         onCardMoved = { cardId, fromCol, toCol, fromIdx, toIdx ->
 *             // Persist changes
 *         },
 *         onCardClick = { card ->
 *             // Show card details
 *         },
 *     ),
 *     cardContent = { card -> ... }
 * )
 * ```
 *
 * Or use the builder DSL:
 *
 * ```kotlin
 * SimpleKBoard(
 *     state = state,
 *     callbacks = simpleKCallbacks {
 *         onCardMoved { cardId, fromCol, toCol, fromIdx, toIdx ->
 *             // Persist changes
 *         }
 *         onCardClick { card ->
 *             // Show card details
 *         }
 *     },
 *     cardContent = { card -> ... }
 * )
 * ```
 *
 * @param T The type of card items in the board
 */
data class SimpleKCallbacks<T : SimpleKItem>(
    /**
     * Called when a card is moved to a new position.
     *
     * This is invoked after the move completes, providing both source and target positions.
     * Use this to persist changes to your data layer.
     *
     * @param cardId The ID of the card that was moved
     * @param fromColumnId The ID of the source column
     * @param toColumnId The ID of the target column
     * @param fromIndex The original index in the source column
     * @param toIndex The new index in the target column
     */
    val onCardMoved: ((cardId: SimpleKId, fromColumnId: SimpleKId, toColumnId: SimpleKId, fromIndex: Int, toIndex: Int) -> Unit)? = null,

    /**
     * Called when a card drag operation begins.
     *
     * Use this for analytics, haptic feedback, or UI state changes.
     *
     * @param item The card being dragged
     */
    val onDragStart: ((item: T) -> Unit)? = null,

    /**
     * Called when a card drag operation ends.
     *
     * This is called whether the drag completed successfully or was cancelled.
     *
     * @param item The card that was being dragged
     * @param cancelled True if the drag was cancelled (e.g., dropped outside valid target)
     */
    val onDragEnd: ((item: T, cancelled: Boolean) -> Unit)? = null,

    /**
     * Called when a card is clicked (tapped without dragging).
     *
     * Use this to show card details, open an editor, etc.
     *
     * @param item The card that was clicked
     */
    val onCardClick: ((item: T) -> Unit)? = null,

    /**
     * Validation callback to allow or deny card moves.
     *
     * Called before a card is moved to a new column. Return `false` to prevent the move.
     * Use this for WIP limits, permission checks, or workflow rules.
     *
     * @param cardId The ID of the card being moved
     * @param toColumnId The ID of the target column
     * @return True to allow the move, false to prevent it
     */
    val canMoveCard: ((cardId: SimpleKId, toColumnId: SimpleKId) -> Boolean)? = null,
) {
    companion object {
        /**
         * Empty callbacks instance (no-op for all events).
         */
        fun <T : SimpleKItem> empty(): SimpleKCallbacks<T> = SimpleKCallbacks()
    }
}

/**
 * Builder for creating [SimpleKCallbacks] with a DSL syntax.
 */
class SimpleKCallbacksBuilder<T : SimpleKItem> {
    private var onCardMoved: ((SimpleKId, SimpleKId, SimpleKId, Int, Int) -> Unit)? = null
    private var onDragStart: ((T) -> Unit)? = null
    private var onDragEnd: ((T, Boolean) -> Unit)? = null
    private var onCardClick: ((T) -> Unit)? = null
    private var canMoveCard: ((SimpleKId, SimpleKId) -> Boolean)? = null

    /**
     * Set the callback for when a card is moved.
     */
    fun onCardMoved(handler: (cardId: SimpleKId, fromColumnId: SimpleKId, toColumnId: SimpleKId, fromIndex: Int, toIndex: Int) -> Unit) {
        onCardMoved = handler
    }

    /**
     * Set the callback for when a drag starts.
     */
    fun onDragStart(handler: (item: T) -> Unit) {
        onDragStart = handler
    }

    /**
     * Set the callback for when a drag ends.
     */
    fun onDragEnd(handler: (item: T, cancelled: Boolean) -> Unit) {
        onDragEnd = handler
    }

    /**
     * Set the callback for when a card is clicked.
     */
    fun onCardClick(handler: (item: T) -> Unit) {
        onCardClick = handler
    }

    /**
     * Set the validation callback for card moves.
     */
    fun canMoveCard(validator: (cardId: SimpleKId, toColumnId: SimpleKId) -> Boolean) {
        canMoveCard = validator
    }

    internal fun build(): SimpleKCallbacks<T> = SimpleKCallbacks(
        onCardMoved = onCardMoved,
        onDragStart = onDragStart,
        onDragEnd = onDragEnd,
        onCardClick = onCardClick,
        canMoveCard = canMoveCard,
    )
}

/**
 * Create [SimpleKCallbacks] using a builder DSL.
 *
 * ```kotlin
 * val callbacks = simpleKCallbacks<MyCard> {
 *     onCardMoved { cardId, fromCol, toCol, fromIdx, toIdx ->
 *         database.updateCardPosition(cardId, toCol, toIdx)
 *     }
 *     onCardClick { card ->
 *         navController.navigate("card/${card.id}")
 *     }
 *     canMoveCard { cardId, toColumnId ->
 *         // Enforce WIP limit
 *         getColumnCardCount(toColumnId) < 5
 *     }
 * }
 * ```
 */
fun <T : SimpleKItem> simpleKCallbacks(
    builder: SimpleKCallbacksBuilder<T>.() -> Unit,
): SimpleKCallbacks<T> = SimpleKCallbacksBuilder<T>().apply(builder).build()
