package io.github.simplek.scope

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import io.github.simplek.model.SimpleKId

/**
 * Scope for card slot content.
 * Provides drag handle modifier and card state information.
 */
@Stable
interface SimpleKCardScope {
    /**
     * Whether this card is currently being dragged.
     */
    val isDragging: Boolean

    /**
     * Whether this card is the drop target for another dragged card.
     */
    val isDropTarget: Boolean

    /**
     * Progress of the drag animation (0f = at rest, 1f = fully lifted).
     */
    val dragProgress: Float

    /**
     * The index of this card within its column (0-based).
     */
    val cardIndex: Int

    /**
     * The ID of the column this card belongs to.
     */
    val columnId: SimpleKId

    /**
     * Apply to make a child composable the drag handle.
     * If not used, the entire card is draggable.
     */
    fun Modifier.dragHandle(): Modifier
}

/**
 * Internal implementation of [SimpleKCardScope].
 */
internal class SimpleKCardScopeImpl(
    override val isDragging: Boolean,
    override val isDropTarget: Boolean,
    override val dragProgress: Float,
    override val cardIndex: Int,
    override val columnId: SimpleKId,
    private val dragHandleModifier: Modifier,
) : SimpleKCardScope {
    override fun Modifier.dragHandle(): Modifier = this.then(dragHandleModifier)
}
