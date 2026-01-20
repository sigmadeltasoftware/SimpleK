package io.github.simplek

import io.github.simplek.model.SimpleKBoard
import io.github.simplek.model.SimpleKColumn
import io.github.simplek.model.SimpleKId
import io.github.simplek.model.SimpleKItem
import io.github.simplek.state.DragState
import io.github.simplek.state.SimpleKState
import io.github.simplek.state.ZoomPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for drag gesture behavior in the Kanban board.
 *
 * Note: Full UI gesture testing requires instrumented tests with Compose testing APIs.
 * These tests verify the state transitions that occur during drag operations.
 */
class DragGestureTest {

    private data class TestItem(
        override val id: SimpleKId,
        val title: String,
    ) : SimpleKItem

    private fun createTestBoard(): SimpleKBoard<TestItem> = SimpleKBoard(
        id = SimpleKId("board-1"),
        columns = listOf(
            SimpleKColumn(
                id = SimpleKId("col-1"),
                title = "To Do",
                items = listOf(
                    TestItem(SimpleKId("card-1"), "Card 1"),
                    TestItem(SimpleKId("card-2"), "Card 2"),
                ),
            ),
            SimpleKColumn(
                id = SimpleKId("col-2"),
                title = "Done",
                items = emptyList(),
            ),
        ),
    )

    private fun createState(): SimpleKState<TestItem> {
        return SimpleKState(
            initialBoard = createTestBoard(),
            coroutineScope = CoroutineScope(Dispatchers.Default),
        )
    }

    // ==================== Drag Start Detection Tests ====================

    @Test
    fun startCardDrag_setsDragStateCorrectly() {
        val state = createState()
        val card = state.board.columns[0].items[0]

        state.startCardDrag(
            item = card,
            columnId = SimpleKId("col-1"),
            index = 0,
            offset = Offset(100f, 100f),
            touchOffsetInCard = Offset(50f, 25f),
        )

        val dragState = state.cardDragState
        assertTrue(dragState is DragState.Dragging)
        assertEquals(card.id, (dragState as DragState.Dragging).item.id)
        assertEquals(SimpleKId("col-1"), dragState.sourceColumnId)
        assertEquals(0, dragState.sourceIndex)
        assertEquals(Offset(100f, 100f), dragState.currentOffset)
        assertEquals(Offset(50f, 25f), dragState.touchOffsetInCard)
    }

    @Test
    fun startCardDrag_setsIsDraggingTrue() {
        val state = createState()
        val card = state.board.columns[0].items[0]

        assertFalse(state.isDragging.value)

        state.startCardDrag(
            item = card,
            columnId = SimpleKId("col-1"),
            index = 0,
            offset = Offset(100f, 100f),
            touchOffsetInCard = Offset.Zero,
        )

        assertTrue(state.isDragging.value)
    }

    // ==================== Drag Update Tests ====================

    @Test
    fun updateCardDrag_updatesOffset() {
        val state = createState()
        val card = state.board.columns[0].items[0]

        state.startCardDrag(
            item = card,
            columnId = SimpleKId("col-1"),
            index = 0,
            offset = Offset(100f, 100f),
            touchOffsetInCard = Offset.Zero,
        )

        state.updateCardDrag(Offset(200f, 200f))

        val dragState = state.cardDragState as DragState.Dragging
        assertEquals(Offset(200f, 200f), dragState.currentOffset)
    }

    @Test
    fun updateCardDrag_withTargetColumn_transitionsToOverDropTarget() {
        val state = createState()
        val card = state.board.columns[0].items[0]

        state.startCardDrag(
            item = card,
            columnId = SimpleKId("col-1"),
            index = 0,
            offset = Offset(100f, 100f),
            touchOffsetInCard = Offset.Zero,
        )

        state.updateCardDrag(
            offset = Offset(200f, 200f),
            targetColumnId = SimpleKId("col-2"),
            targetIndex = 0,
        )

        assertTrue(state.cardDragState is DragState.OverDropTarget)
        val dragState = state.cardDragState as DragState.OverDropTarget
        assertEquals(SimpleKId("col-2"), dragState.targetColumnId)
        assertEquals(0, dragState.targetIndex)
    }

    @Test
    fun updateCardDrag_clearingTarget_transitionsBackToDragging() {
        val state = createState()
        val card = state.board.columns[0].items[0]

        state.startCardDrag(
            item = card,
            columnId = SimpleKId("col-1"),
            index = 0,
            offset = Offset(100f, 100f),
            touchOffsetInCard = Offset.Zero,
        )

        // First, move over a target
        state.updateCardDrag(
            offset = Offset(200f, 200f),
            targetColumnId = SimpleKId("col-2"),
            targetIndex = 0,
        )
        assertTrue(state.cardDragState is DragState.OverDropTarget)

        // Then, move away from target
        state.updateCardDrag(
            offset = Offset(300f, 300f),
            targetColumnId = null,
            targetIndex = null,
        )
        assertTrue(state.cardDragState is DragState.Dragging)
    }

    // ==================== Drag End Tests ====================

    @Test
    fun endCardDrag_resetsToIdle() {
        val state = createState()
        val card = state.board.columns[0].items[0]

        state.startCardDrag(
            item = card,
            columnId = SimpleKId("col-1"),
            index = 0,
            offset = Offset(100f, 100f),
            touchOffsetInCard = Offset.Zero,
        )

        val result = state.endCardDrag()

        assertTrue(result) // Was dragging
        assertTrue(state.cardDragState is DragState.Idle)
        assertFalse(state.isDragging.value)
    }

    @Test
    fun endCardDrag_whenNotDragging_returnsFalse() {
        val state = createState()

        val result = state.endCardDrag()

        assertFalse(result)
    }

    @Test
    fun cancelCardDrag_resetsToIdle() {
        val state = createState()
        val card = state.board.columns[0].items[0]

        state.startCardDrag(
            item = card,
            columnId = SimpleKId("col-1"),
            index = 0,
            offset = Offset(100f, 100f),
            touchOffsetInCard = Offset.Zero,
        )

        state.cancelCardDrag()

        assertTrue(state.cardDragState is DragState.Idle)
    }

    // ==================== Zoom Phase Tests ====================

    @Test
    fun zoomPhase_initiallyNormal() {
        val state = createState()

        assertEquals(ZoomPhase.Normal, state.zoomPhase)
    }

    @Test
    fun isZoomedOrTransitioning_falseWhenNormal() {
        val state = createState()

        assertFalse(state.isZoomedOrTransitioning)
    }

    // ==================== Pointer Down State Tests ====================

    @Test
    fun setPointerDownOnCard_updatesShouldDisableScroll() {
        val state = createState()

        assertFalse(state.shouldDisableScroll.value)
        assertFalse(state.isPointerDownOnCard)

        state.setPointerDownOnCard(true)

        assertTrue(state.isPointerDownOnCard)
        assertTrue(state.shouldDisableScroll.value)

        state.setPointerDownOnCard(false)

        assertFalse(state.isPointerDownOnCard)
        assertFalse(state.shouldDisableScroll.value)
    }

    // ==================== Hovered Overlay Column Tests ====================

    @Test
    fun setHoveredOverlayColumn_updatesState() {
        val state = createState()

        assertEquals(null, state.hoveredOverlayColumnId)

        state.setHoveredOverlayColumn(SimpleKId("col-1"))

        assertEquals(SimpleKId("col-1"), state.hoveredOverlayColumnId)

        state.clearHoveredOverlayColumn()

        assertEquals(null, state.hoveredOverlayColumnId)
    }

    @Test
    fun endCardDrag_withHoveredOverlayColumn_movesCardToColumn() {
        val state = createState()
        val card = state.board.columns[0].items[0]

        state.startCardDrag(
            item = card,
            columnId = SimpleKId("col-1"),
            index = 0,
            offset = Offset(100f, 100f),
            touchOffsetInCard = Offset.Zero,
        )

        state.setHoveredOverlayColumn(SimpleKId("col-2"))

        state.endCardDrag()

        // Card should have moved to col-2
        val col1 = state.board.columns.first { it.id == SimpleKId("col-1") }
        val col2 = state.board.columns.first { it.id == SimpleKId("col-2") }

        assertEquals(1, col1.items.size)
        assertFalse(col1.items.any { it.id == card.id })
        assertEquals(1, col2.items.size)
        assertTrue(col2.items.any { it.id == card.id })
    }

    // ==================== Drop Card To Column Tests ====================

    @Test
    fun dropCardToColumn_movesCardToTargetColumn() {
        val state = createState()
        val card = state.board.columns[0].items[0]

        state.startCardDrag(
            item = card,
            columnId = SimpleKId("col-1"),
            index = 0,
            offset = Offset(100f, 100f),
            touchOffsetInCard = Offset.Zero,
        )

        val result = state.dropCardToColumn(SimpleKId("col-2"))

        assertTrue(result)
        assertTrue(state.cardDragState is DragState.Idle)

        val col2 = state.board.columns.first { it.id == SimpleKId("col-2") }
        assertEquals(1, col2.items.size)
        assertEquals(card.id, col2.items[0].id)
    }

    @Test
    fun dropCardToColumn_invalidColumn_returnsFalse() {
        val state = createState()
        val card = state.board.columns[0].items[0]

        state.startCardDrag(
            item = card,
            columnId = SimpleKId("col-1"),
            index = 0,
            offset = Offset(100f, 100f),
            touchOffsetInCard = Offset.Zero,
        )

        val result = state.dropCardToColumn(SimpleKId("non-existent"))

        assertFalse(result)
    }

    @Test
    fun dropCardToColumn_whenNotDragging_returnsFalse() {
        val state = createState()

        val result = state.dropCardToColumn(SimpleKId("col-2"))

        assertFalse(result)
    }

    // ==================== Column Drag Tests ====================

    @Test
    fun startColumnDrag_setsColumnDragState() {
        val state = createState()

        state.startColumnDrag(
            columnId = SimpleKId("col-1"),
            index = 0,
            offset = Offset(100f, 100f),
        )

        assertTrue(state.isDragging.value)
        assertTrue(state.columnDragState is io.github.simplek.state.ColumnDragState.Dragging)
    }

    @Test
    fun updateColumnDrag_updatesOffset() {
        val state = createState()

        state.startColumnDrag(
            columnId = SimpleKId("col-1"),
            index = 0,
            offset = Offset(100f, 100f),
        )

        state.updateColumnDrag(Offset(200f, 200f))

        val dragState = state.columnDragState as io.github.simplek.state.ColumnDragState.Dragging
        assertEquals(Offset(200f, 200f), dragState.currentOffset)
    }

    @Test
    fun endColumnDrag_resetsToIdle() {
        val state = createState()

        state.startColumnDrag(
            columnId = SimpleKId("col-1"),
            index = 0,
            offset = Offset(100f, 100f),
        )

        state.endColumnDrag()

        assertTrue(state.columnDragState is io.github.simplek.state.ColumnDragState.Idle)
        assertFalse(state.isDragging.value)
    }

    // ==================== Touch Offset Preservation Tests ====================

    @Test
    fun dragState_preservesTouchOffset_throughUpdates() {
        val state = createState()
        val card = state.board.columns[0].items[0]
        val originalTouchOffset = Offset(50f, 25f)

        state.startCardDrag(
            item = card,
            columnId = SimpleKId("col-1"),
            index = 0,
            offset = Offset(100f, 100f),
            touchOffsetInCard = originalTouchOffset,
        )

        // Update position multiple times
        state.updateCardDrag(Offset(200f, 200f))
        state.updateCardDrag(Offset(300f, 300f))

        val dragState = state.cardDragState as DragState.Dragging
        assertEquals(originalTouchOffset, dragState.touchOffsetInCard)
    }

    @Test
    fun dragState_preservesTouchOffset_whenTransitioningToOverDropTarget() {
        val state = createState()
        val card = state.board.columns[0].items[0]
        val originalTouchOffset = Offset(50f, 25f)

        state.startCardDrag(
            item = card,
            columnId = SimpleKId("col-1"),
            index = 0,
            offset = Offset(100f, 100f),
            touchOffsetInCard = originalTouchOffset,
        )

        state.updateCardDrag(
            offset = Offset(200f, 200f),
            targetColumnId = SimpleKId("col-2"),
            targetIndex = 0,
        )

        val dragState = state.cardDragState as DragState.OverDropTarget
        assertEquals(originalTouchOffset, dragState.touchOffsetInCard)
    }
}
