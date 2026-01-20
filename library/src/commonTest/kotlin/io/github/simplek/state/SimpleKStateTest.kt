package io.github.simplek.state

import io.github.simplek.model.SimpleKBoard
import io.github.simplek.model.SimpleKColumn
import io.github.simplek.model.SimpleKId
import io.github.simplek.model.SimpleKItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [SimpleKState].
 * Tests core state management operations for the Kanban board.
 */
class SimpleKStateTest {

    // Simple test item implementation
    private data class TestItem(
        override val id: SimpleKId,
        val title: String,
    ) : SimpleKItem

    private fun createTestBoard(
        columns: List<SimpleKColumn<TestItem>> = listOf(
            SimpleKColumn(
                id = SimpleKId("col-1"),
                title = "To Do",
                items = listOf(
                    TestItem(SimpleKId("card-1"), "Card 1"),
                    TestItem(SimpleKId("card-2"), "Card 2"),
                    TestItem(SimpleKId("card-3"), "Card 3"),
                ),
            ),
            SimpleKColumn(
                id = SimpleKId("col-2"),
                title = "In Progress",
                items = listOf(
                    TestItem(SimpleKId("card-4"), "Card 4"),
                ),
            ),
            SimpleKColumn(
                id = SimpleKId("col-3"),
                title = "Done",
                items = emptyList(),
            ),
        ),
    ): SimpleKBoard<TestItem> = SimpleKBoard(
        id = SimpleKId("board-1"),
        columns = columns,
    )

    private fun createState(board: SimpleKBoard<TestItem> = createTestBoard()): SimpleKState<TestItem> {
        return SimpleKState(
            initialBoard = board,
            coroutineScope = CoroutineScope(Dispatchers.Default),
        )
    }

    // ==================== moveCard() Tests ====================

    @Test
    fun moveCard_withinSameColumn_movesCardToNewPosition() {
        val state = createState()

        // Move card-3 from index 2 to index 0 within col-1
        state.moveCard(
            cardId = SimpleKId("card-3"),
            toColumnId = SimpleKId("col-1"),
            toIndex = 0,
        )

        val column = state.board.columns.first { it.id == SimpleKId("col-1") }
        assertEquals(3, column.items.size)
        assertEquals("card-3", column.items[0].id.value)
        assertEquals("card-1", column.items[1].id.value)
        assertEquals("card-2", column.items[2].id.value)
    }

    @Test
    fun moveCard_withinSameColumn_movesToEndPosition() {
        val state = createState()

        // Move card-1 from index 0 to index 2 within col-1
        state.moveCard(
            cardId = SimpleKId("card-1"),
            toColumnId = SimpleKId("col-1"),
            toIndex = 2,
        )

        val column = state.board.columns.first { it.id == SimpleKId("col-1") }
        assertEquals(3, column.items.size)
        assertEquals("card-2", column.items[0].id.value)
        assertEquals("card-3", column.items[1].id.value)
        assertEquals("card-1", column.items[2].id.value)
    }

    @Test
    fun moveCard_toDifferentColumn_removesFromSourceAndAddsToTarget() {
        val state = createState()

        // Move card-1 from col-1 to col-2
        state.moveCard(
            cardId = SimpleKId("card-1"),
            toColumnId = SimpleKId("col-2"),
            toIndex = 0,
        )

        val sourceColumn = state.board.columns.first { it.id == SimpleKId("col-1") }
        val targetColumn = state.board.columns.first { it.id == SimpleKId("col-2") }

        assertEquals(2, sourceColumn.items.size)
        assertFalse(sourceColumn.items.any { it.id.value == "card-1" })

        assertEquals(2, targetColumn.items.size)
        assertEquals("card-1", targetColumn.items[0].id.value)
        assertEquals("card-4", targetColumn.items[1].id.value)
    }

    @Test
    fun moveCard_toEmptyColumn_addsCardSuccessfully() {
        val state = createState()

        // Move card-1 to empty col-3
        state.moveCard(
            cardId = SimpleKId("card-1"),
            toColumnId = SimpleKId("col-3"),
            toIndex = 0,
        )

        val targetColumn = state.board.columns.first { it.id == SimpleKId("col-3") }
        assertEquals(1, targetColumn.items.size)
        assertEquals("card-1", targetColumn.items[0].id.value)
    }

    @Test
    fun moveCard_withInvalidCardId_doesNotCrash() {
        val state = createState()
        val originalBoard = state.board

        // Move non-existent card - should not crash
        state.moveCard(
            cardId = SimpleKId("non-existent"),
            toColumnId = SimpleKId("col-1"),
            toIndex = 0,
        )

        // Board columns should remain unchanged in size
        assertEquals(originalBoard.columns.size, state.board.columns.size)
    }

    @Test
    fun moveCard_withInvalidColumnId_doesNotAddCard() {
        val state = createState()

        // Move card to non-existent column - card is removed but not added anywhere
        state.moveCard(
            cardId = SimpleKId("card-1"),
            toColumnId = SimpleKId("non-existent"),
            toIndex = 0,
        )

        val sourceColumn = state.board.columns.first { it.id == SimpleKId("col-1") }
        // Card should be removed from source
        assertEquals(2, sourceColumn.items.size)
    }

    @Test
    fun moveCard_withIndexOutOfBounds_clampsToValidRange() {
        val state = createState()

        // Move card with index > list size
        state.moveCard(
            cardId = SimpleKId("card-1"),
            toColumnId = SimpleKId("col-2"),
            toIndex = 100,
        )

        val targetColumn = state.board.columns.first { it.id == SimpleKId("col-2") }
        // Should be clamped to end of list
        assertEquals(2, targetColumn.items.size)
        assertEquals("card-1", targetColumn.items.last().id.value)
    }

    // ==================== addCard() Tests ====================

    @Test
    fun addCard_withoutIndex_addsToEnd() {
        val state = createState()
        val newCard = TestItem(SimpleKId("card-new"), "New Card")

        state.addCard(SimpleKId("col-1"), newCard)

        val column = state.board.columns.first { it.id == SimpleKId("col-1") }
        assertEquals(4, column.items.size)
        assertEquals("card-new", column.items.last().id.value)
    }

    @Test
    fun addCard_withIndex_insertsAtPosition() {
        val state = createState()
        val newCard = TestItem(SimpleKId("card-new"), "New Card")

        state.addCard(SimpleKId("col-1"), newCard, index = 1)

        val column = state.board.columns.first { it.id == SimpleKId("col-1") }
        assertEquals(4, column.items.size)
        assertEquals("card-1", column.items[0].id.value)
        assertEquals("card-new", column.items[1].id.value)
        assertEquals("card-2", column.items[2].id.value)
    }

    @Test
    fun addCard_toEmptyColumn_addsSuccessfully() {
        val state = createState()
        val newCard = TestItem(SimpleKId("card-new"), "New Card")

        state.addCard(SimpleKId("col-3"), newCard)

        val column = state.board.columns.first { it.id == SimpleKId("col-3") }
        assertEquals(1, column.items.size)
        assertEquals("card-new", column.items[0].id.value)
    }

    @Test
    fun addCard_withNegativeIndex_insertsAtBeginning() {
        val state = createState()
        val newCard = TestItem(SimpleKId("card-new"), "New Card")

        state.addCard(SimpleKId("col-1"), newCard, index = -5)

        val column = state.board.columns.first { it.id == SimpleKId("col-1") }
        assertEquals(4, column.items.size)
        assertEquals("card-new", column.items[0].id.value)
    }

    @Test
    fun addCard_withIndexBeyondSize_insertsAtEnd() {
        val state = createState()
        val newCard = TestItem(SimpleKId("card-new"), "New Card")

        state.addCard(SimpleKId("col-1"), newCard, index = 100)

        val column = state.board.columns.first { it.id == SimpleKId("col-1") }
        assertEquals(4, column.items.size)
        assertEquals("card-new", column.items.last().id.value)
    }

    @Test
    fun addCard_toInvalidColumn_doesNotAdd() {
        val state = createState()
        val newCard = TestItem(SimpleKId("card-new"), "New Card")

        state.addCard(SimpleKId("non-existent"), newCard)

        // All columns should have same size as before
        assertEquals(3, state.board.columns[0].items.size)
        assertEquals(1, state.board.columns[1].items.size)
        assertEquals(0, state.board.columns[2].items.size)
    }

    // ==================== removeCard() Tests ====================

    @Test
    fun removeCard_existingCard_removesSuccessfully() {
        val state = createState()

        state.removeCard(SimpleKId("col-1"), SimpleKId("card-2"))

        val column = state.board.columns.first { it.id == SimpleKId("col-1") }
        assertEquals(2, column.items.size)
        assertEquals("card-1", column.items[0].id.value)
        assertEquals("card-3", column.items[1].id.value)
    }

    @Test
    fun removeCard_onlyCardInColumn_leavesColumnEmpty() {
        val state = createState()

        state.removeCard(SimpleKId("col-2"), SimpleKId("card-4"))

        val column = state.board.columns.first { it.id == SimpleKId("col-2") }
        assertTrue(column.items.isEmpty())
    }

    @Test
    fun removeCard_nonExistentCard_doesNotAffectColumn() {
        val state = createState()

        state.removeCard(SimpleKId("col-1"), SimpleKId("non-existent"))

        val column = state.board.columns.first { it.id == SimpleKId("col-1") }
        assertEquals(3, column.items.size)
    }

    @Test
    fun removeCard_wrongColumnId_doesNotRemoveCard() {
        val state = createState()

        // Try to remove card-1 from col-2 (it's in col-1)
        state.removeCard(SimpleKId("col-2"), SimpleKId("card-1"))

        val col1 = state.board.columns.first { it.id == SimpleKId("col-1") }
        assertEquals(3, col1.items.size)
        assertTrue(col1.items.any { it.id.value == "card-1" })
    }

    // ==================== addColumn() Tests ====================

    @Test
    fun addColumn_withoutIndex_addsToEnd() {
        val state = createState()
        val newColumn = SimpleKColumn<TestItem>(
            id = SimpleKId("col-new"),
            title = "New Column",
            items = emptyList(),
        )

        state.addColumn(newColumn)

        assertEquals(4, state.board.columns.size)
        assertEquals("col-new", state.board.columns.last().id.value)
    }

    @Test
    fun addColumn_withIndex_insertsAtPosition() {
        val state = createState()
        val newColumn = SimpleKColumn<TestItem>(
            id = SimpleKId("col-new"),
            title = "New Column",
            items = emptyList(),
        )

        state.addColumn(newColumn, index = 1)

        assertEquals(4, state.board.columns.size)
        assertEquals("col-1", state.board.columns[0].id.value)
        assertEquals("col-new", state.board.columns[1].id.value)
        assertEquals("col-2", state.board.columns[2].id.value)
    }

    @Test
    fun addColumn_atBeginning_insertsFirst() {
        val state = createState()
        val newColumn = SimpleKColumn<TestItem>(
            id = SimpleKId("col-new"),
            title = "New Column",
            items = emptyList(),
        )

        state.addColumn(newColumn, index = 0)

        assertEquals(4, state.board.columns.size)
        assertEquals("col-new", state.board.columns.first().id.value)
    }

    @Test
    fun addColumn_withNegativeIndex_insertsAtBeginning() {
        val state = createState()
        val newColumn = SimpleKColumn<TestItem>(
            id = SimpleKId("col-new"),
            title = "New Column",
            items = emptyList(),
        )

        state.addColumn(newColumn, index = -5)

        assertEquals(4, state.board.columns.size)
        assertEquals("col-new", state.board.columns.first().id.value)
    }

    @Test
    fun addColumn_withCards_addsColumnWithCards() {
        val state = createState()
        val newColumn = SimpleKColumn(
            id = SimpleKId("col-new"),
            title = "New Column",
            items = listOf(
                TestItem(SimpleKId("card-new-1"), "New Card 1"),
                TestItem(SimpleKId("card-new-2"), "New Card 2"),
            ),
        )

        state.addColumn(newColumn)

        val addedColumn = state.board.columns.last()
        assertEquals(2, addedColumn.items.size)
        assertEquals("card-new-1", addedColumn.items[0].id.value)
    }

    // ==================== removeColumn() Tests ====================

    @Test
    fun removeColumn_existingColumn_removesSuccessfully() {
        val state = createState()

        state.removeColumn(SimpleKId("col-2"))

        assertEquals(2, state.board.columns.size)
        assertFalse(state.board.columns.any { it.id.value == "col-2" })
    }

    @Test
    fun removeColumn_firstColumn_removesCorrectly() {
        val state = createState()

        state.removeColumn(SimpleKId("col-1"))

        assertEquals(2, state.board.columns.size)
        assertEquals("col-2", state.board.columns.first().id.value)
    }

    @Test
    fun removeColumn_lastColumn_removesCorrectly() {
        val state = createState()

        state.removeColumn(SimpleKId("col-3"))

        assertEquals(2, state.board.columns.size)
        assertEquals("col-2", state.board.columns.last().id.value)
    }

    @Test
    fun removeColumn_nonExistent_doesNotAffectBoard() {
        val state = createState()

        state.removeColumn(SimpleKId("non-existent"))

        assertEquals(3, state.board.columns.size)
    }

    @Test
    fun removeColumn_columnWithCards_removesColumnAndCards() {
        val state = createState()

        state.removeColumn(SimpleKId("col-1"))

        assertEquals(2, state.board.columns.size)
        // Cards in col-1 are gone
        val allCardIds = state.board.columns.flatMap { it.items }.map { it.id.value }
        assertFalse(allCardIds.contains("card-1"))
        assertFalse(allCardIds.contains("card-2"))
        assertFalse(allCardIds.contains("card-3"))
    }

    // ==================== moveColumn() Tests ====================

    @Test
    fun moveColumn_fromStartToEnd_reordersCorrectly() {
        val state = createState()

        state.moveColumn(fromIndex = 0, toIndex = 2)

        assertEquals("col-2", state.board.columns[0].id.value)
        assertEquals("col-3", state.board.columns[1].id.value)
        assertEquals("col-1", state.board.columns[2].id.value)
    }

    @Test
    fun moveColumn_fromEndToStart_reordersCorrectly() {
        val state = createState()

        state.moveColumn(fromIndex = 2, toIndex = 0)

        assertEquals("col-3", state.board.columns[0].id.value)
        assertEquals("col-1", state.board.columns[1].id.value)
        assertEquals("col-2", state.board.columns[2].id.value)
    }

    @Test
    fun moveColumn_sameIndex_noChange() {
        val state = createState()

        state.moveColumn(fromIndex = 1, toIndex = 1)

        assertEquals("col-1", state.board.columns[0].id.value)
        assertEquals("col-2", state.board.columns[1].id.value)
        assertEquals("col-3", state.board.columns[2].id.value)
    }

    // ==================== updateBoard() Tests ====================

    @Test
    fun updateBoard_replacesEntireBoard() {
        val state = createState()
        val newBoard = SimpleKBoard<TestItem>(
            id = SimpleKId("board-new"),
            columns = listOf(
                SimpleKColumn(
                    id = SimpleKId("col-x"),
                    title = "Column X",
                    items = emptyList(),
                ),
            ),
        )

        state.updateBoard(newBoard)

        assertEquals("board-new", state.board.id.value)
        assertEquals(1, state.board.columns.size)
        assertEquals("col-x", state.board.columns[0].id.value)
    }

    // ==================== Drag State Tests ====================

    @Test
    fun isDragging_initiallyFalse() {
        val state = createState()

        assertFalse(state.isDragging.value)
    }

    @Test
    fun cardDragState_initiallyIdle() {
        val state = createState()

        assertTrue(state.cardDragState is DragState.Idle)
    }

    // ==================== Edge Cases ====================

    @Test
    fun emptyBoard_operationsDoNotCrash() {
        val emptyBoard = SimpleKBoard<TestItem>(
            id = SimpleKId("empty-board"),
            columns = emptyList(),
        )
        val state = createState(emptyBoard)

        // These should not crash
        state.moveCard(SimpleKId("any"), SimpleKId("any"), 0)
        state.removeCard(SimpleKId("any"), SimpleKId("any"))
        state.removeColumn(SimpleKId("any"))

        assertTrue(state.board.columns.isEmpty())
    }

    @Test
    fun addColumn_toEmptyBoard_succeeds() {
        val emptyBoard = SimpleKBoard<TestItem>(
            id = SimpleKId("empty-board"),
            columns = emptyList(),
        )
        val state = createState(emptyBoard)

        state.addColumn(
            SimpleKColumn(
                id = SimpleKId("col-1"),
                title = "First Column",
                items = emptyList(),
            )
        )

        assertEquals(1, state.board.columns.size)
    }

    // ==================== Undo/Redo Tests ====================

    @Test
    fun canUndo_initiallyFalse() {
        val state = createState()

        assertFalse(state.canUndo.value)
    }

    @Test
    fun canRedo_initiallyFalse() {
        val state = createState()

        assertFalse(state.canRedo.value)
    }

    @Test
    fun undo_afterMoveCard_restoresOriginalPosition() {
        val state = createState()

        // Move card-1 from col-1 to col-2
        state.moveCard(
            cardId = SimpleKId("card-1"),
            toColumnId = SimpleKId("col-2"),
            toIndex = 0,
        )

        // Verify card was moved
        val col2AfterMove = state.board.columns.first { it.id == SimpleKId("col-2") }
        assertEquals(2, col2AfterMove.items.size)
        assertTrue(state.canUndo.value)

        // Undo
        state.undo()

        // Verify card is back in original position
        val col1AfterUndo = state.board.columns.first { it.id == SimpleKId("col-1") }
        val col2AfterUndo = state.board.columns.first { it.id == SimpleKId("col-2") }
        assertEquals(3, col1AfterUndo.items.size)
        assertEquals(1, col2AfterUndo.items.size)
        assertEquals("card-1", col1AfterUndo.items[0].id.value)
    }

    @Test
    fun redo_afterUndo_restoresMovedPosition() {
        val state = createState()

        // Move card-1 from col-1 to col-2
        state.moveCard(
            cardId = SimpleKId("card-1"),
            toColumnId = SimpleKId("col-2"),
            toIndex = 0,
        )

        // Undo
        state.undo()

        // Verify can redo
        assertTrue(state.canRedo.value)

        // Redo
        state.redo()

        // Verify card is moved again
        val col1AfterRedo = state.board.columns.first { it.id == SimpleKId("col-1") }
        val col2AfterRedo = state.board.columns.first { it.id == SimpleKId("col-2") }
        assertEquals(2, col1AfterRedo.items.size)
        assertEquals(2, col2AfterRedo.items.size)
        assertEquals("card-1", col2AfterRedo.items[0].id.value)
    }

    @Test
    fun undo_afterAddCard_removesCard() {
        val state = createState()
        val newCard = TestItem(SimpleKId("card-new"), "New Card")

        state.addCard(SimpleKId("col-1"), newCard)

        val colAfterAdd = state.board.columns.first { it.id == SimpleKId("col-1") }
        assertEquals(4, colAfterAdd.items.size)

        // Undo
        state.undo()

        val colAfterUndo = state.board.columns.first { it.id == SimpleKId("col-1") }
        assertEquals(3, colAfterUndo.items.size)
        assertFalse(colAfterUndo.items.any { it.id.value == "card-new" })
    }

    @Test
    fun undo_afterRemoveCard_restoresCard() {
        val state = createState()

        state.removeCard(SimpleKId("col-1"), SimpleKId("card-2"))

        val colAfterRemove = state.board.columns.first { it.id == SimpleKId("col-1") }
        assertEquals(2, colAfterRemove.items.size)

        // Undo
        state.undo()

        val colAfterUndo = state.board.columns.first { it.id == SimpleKId("col-1") }
        assertEquals(3, colAfterUndo.items.size)
        assertEquals("card-2", colAfterUndo.items[1].id.value)
    }

    @Test
    fun undo_afterAddColumn_removesColumn() {
        val state = createState()
        val newColumn = SimpleKColumn<TestItem>(
            id = SimpleKId("col-new"),
            title = "New Column",
            items = emptyList(),
        )

        state.addColumn(newColumn)
        assertEquals(4, state.board.columns.size)

        // Undo
        state.undo()

        assertEquals(3, state.board.columns.size)
        assertFalse(state.board.columns.any { it.id.value == "col-new" })
    }

    @Test
    fun undo_afterRemoveColumn_restoresColumn() {
        val state = createState()

        state.removeColumn(SimpleKId("col-2"))
        assertEquals(2, state.board.columns.size)

        // Undo
        state.undo()

        assertEquals(3, state.board.columns.size)
        val restoredColumn = state.board.columns.find { it.id.value == "col-2" }
        assertNotNull(restoredColumn)
        assertEquals("In Progress", restoredColumn?.title)
        assertEquals(1, restoredColumn?.items?.size)
    }

    @Test
    fun historyLimit_enforcedWhenExceeded() {
        // Create state with small history limit
        val board = createTestBoard()
        val state = SimpleKState(
            initialBoard = board,
            coroutineScope = CoroutineScope(Dispatchers.Default),
            maxHistorySize = 3,
        )

        // Make 5 moves
        repeat(5) { i ->
            state.moveCard(
                cardId = SimpleKId("card-1"),
                toColumnId = if (i % 2 == 0) SimpleKId("col-2") else SimpleKId("col-1"),
                toIndex = 0,
            )
        }

        // Should only be able to undo 3 times (history limit)
        var undoCount = 0
        while (state.canUndo.value) {
            state.undo()
            undoCount++
        }

        assertEquals(3, undoCount)
    }

    @Test
    fun clearHistory_clearsUndoAndRedo() {
        val state = createState()

        // Make some changes
        state.moveCard(SimpleKId("card-1"), SimpleKId("col-2"), 0)
        state.undo()

        assertTrue(state.canRedo.value)

        // Make another change
        state.moveCard(SimpleKId("card-2"), SimpleKId("col-2"), 0)
        assertTrue(state.canUndo.value)

        // Clear history
        state.clearHistory()

        assertFalse(state.canUndo.value)
        assertFalse(state.canRedo.value)
    }

    @Test
    fun updateBoard_clearsHistory() {
        val state = createState()

        // Make some changes
        state.moveCard(SimpleKId("card-1"), SimpleKId("col-2"), 0)
        assertTrue(state.canUndo.value)

        // Update board (replaces entire board)
        state.updateBoard(createTestBoard())

        // History should be cleared
        assertFalse(state.canUndo.value)
        assertFalse(state.canRedo.value)
    }

    @Test
    fun newAction_clearsRedoStack() {
        val state = createState()

        // Move card, then undo
        state.moveCard(SimpleKId("card-1"), SimpleKId("col-2"), 0)
        state.undo()
        assertTrue(state.canRedo.value)

        // Make a new action
        state.moveCard(SimpleKId("card-2"), SimpleKId("col-2"), 0)

        // Redo stack should be cleared
        assertFalse(state.canRedo.value)
    }

    @Test
    fun multipleUndoRedo_worksCorrectly() {
        val state = createState()

        // Make 3 moves
        state.moveCard(SimpleKId("card-1"), SimpleKId("col-2"), 0)
        state.moveCard(SimpleKId("card-2"), SimpleKId("col-2"), 0)
        state.moveCard(SimpleKId("card-3"), SimpleKId("col-2"), 0)

        // Verify all cards moved
        val col2 = state.board.columns.first { it.id == SimpleKId("col-2") }
        assertEquals(4, col2.items.size)

        // Undo all 3
        state.undo()
        state.undo()
        state.undo()

        // Verify all cards back
        val col1AfterUndo = state.board.columns.first { it.id == SimpleKId("col-1") }
        val col2AfterUndo = state.board.columns.first { it.id == SimpleKId("col-2") }
        assertEquals(3, col1AfterUndo.items.size)
        assertEquals(1, col2AfterUndo.items.size)

        // Redo 2 of them
        state.redo()
        state.redo()

        val col2AfterPartialRedo = state.board.columns.first { it.id == SimpleKId("col-2") }
        assertEquals(3, col2AfterPartialRedo.items.size)
    }

    @Test
    fun undo_withEmptyStack_doesNothing() {
        val state = createState()

        // Undo on empty stack should not crash
        state.undo()

        // Board should be unchanged
        assertEquals(3, state.board.columns[0].items.size)
    }

    @Test
    fun redo_withEmptyStack_doesNothing() {
        val state = createState()

        // Redo on empty stack should not crash
        state.redo()

        // Board should be unchanged
        assertEquals(3, state.board.columns[0].items.size)
    }

    private fun assertNotNull(value: Any?) {
        assertTrue(value != null, "Expected non-null value")
    }
}
