package io.github.simplek.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import io.github.simplek.model.SimpleKId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [DropZoneRegistry].
 * Tests drop zone registration, hit detection, and cleanup.
 */
class DropZoneRegistryTest {

    private fun createRegistry() = DropZoneRegistry()

    // ==================== registerCard() Tests ====================

    @Test
    fun registerCard_addsCardToRegistry() {
        val registry = createRegistry()

        registry.registerCard(
            cardId = SimpleKId("card-1"),
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(100f, 50f)),
        )

        assertEquals(1, registry.cardZones.size)
        assertTrue(registry.cardZones.containsKey("col-1:card-1"))
    }

    @Test
    fun registerCard_updatesExistingCard() {
        val registry = createRegistry()

        registry.registerCard(
            cardId = SimpleKId("card-1"),
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(100f, 50f)),
        )

        registry.registerCard(
            cardId = SimpleKId("card-1"),
            columnId = SimpleKId("col-1"),
            index = 1,
            bounds = Rect(offset = Offset(0f, 50f), size = Size(100f, 50f)),
        )

        assertEquals(1, registry.cardZones.size)
        assertEquals(1, registry.cardZones["col-1:card-1"]?.index)
    }

    @Test
    fun registerCard_multipleCards_registersAll() {
        val registry = createRegistry()

        registry.registerCard(
            cardId = SimpleKId("card-1"),
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(100f, 50f)),
        )
        registry.registerCard(
            cardId = SimpleKId("card-2"),
            columnId = SimpleKId("col-1"),
            index = 1,
            bounds = Rect(offset = Offset(0f, 60f), size = Size(100f, 50f)),
        )
        registry.registerCard(
            cardId = SimpleKId("card-3"),
            columnId = SimpleKId("col-2"),
            index = 0,
            bounds = Rect(offset = Offset(120f, 0f), size = Size(100f, 50f)),
        )

        assertEquals(3, registry.cardZones.size)
    }

    // ==================== unregisterCard() Tests ====================

    @Test
    fun unregisterCard_removesCardFromRegistry() {
        val registry = createRegistry()

        registry.registerCard(
            cardId = SimpleKId("card-1"),
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(100f, 50f)),
        )

        registry.unregisterCard(SimpleKId("card-1"), SimpleKId("col-1"))

        assertTrue(registry.cardZones.isEmpty())
    }

    @Test
    fun unregisterCard_nonExistent_doesNotCrash() {
        val registry = createRegistry()

        registry.unregisterCard(SimpleKId("non-existent"), SimpleKId("col-1"))

        assertTrue(registry.cardZones.isEmpty())
    }

    // ==================== registerColumn() Tests ====================

    @Test
    fun registerColumn_addsColumnToRegistry() {
        val registry = createRegistry()

        registry.registerColumn(
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(200f, 500f)),
        )

        assertEquals(1, registry.columnZones.size)
        assertTrue(registry.columnZones.containsKey(SimpleKId("col-1")))
    }

    @Test
    fun registerColumn_multipleColumns_registersAll() {
        val registry = createRegistry()

        registry.registerColumn(
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(200f, 500f)),
        )
        registry.registerColumn(
            columnId = SimpleKId("col-2"),
            index = 1,
            bounds = Rect(offset = Offset(220f, 0f), size = Size(200f, 500f)),
        )

        assertEquals(2, registry.columnZones.size)
    }

    // ==================== unregisterColumn() Tests ====================

    @Test
    fun unregisterColumn_removesColumnFromRegistry() {
        val registry = createRegistry()

        registry.registerColumn(
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(200f, 500f)),
        )

        registry.unregisterColumn(SimpleKId("col-1"))

        assertTrue(registry.columnZones.isEmpty())
    }

    // ==================== findDropTarget() Tests ====================

    @Test
    fun findDropTarget_overColumn_returnsTarget() {
        val registry = createRegistry()

        // Setup column
        registry.registerColumn(
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(200f, 500f)),
        )

        // Setup cards in column
        registry.registerCard(
            cardId = SimpleKId("card-1"),
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(10f, 10f), size = Size(180f, 50f)),
        )
        registry.registerCard(
            cardId = SimpleKId("card-2"),
            columnId = SimpleKId("col-1"),
            index = 1,
            bounds = Rect(offset = Offset(10f, 70f), size = Size(180f, 50f)),
        )

        // Drag card-3 from different column over position between card-1 and card-2
        val target = registry.findDropTarget(
            position = Offset(100f, 65f), // Between card-1 and card-2
            draggedCardId = SimpleKId("card-3"),
            draggedFromColumnId = SimpleKId("col-2"),
        )

        assertNotNull(target)
        assertEquals("col-1", target.columnId.value)
        // Should be index 1 (after card-1, before card-2)
        assertEquals(1, target.index)
    }

    @Test
    fun findDropTarget_aboveFirstCard_returnsIndex0() {
        val registry = createRegistry()

        registry.registerColumn(
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(200f, 500f)),
        )
        registry.registerCard(
            cardId = SimpleKId("card-1"),
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(10f, 50f), size = Size(180f, 50f)),
        )

        val target = registry.findDropTarget(
            position = Offset(100f, 30f), // Above card-1
            draggedCardId = SimpleKId("card-2"),
            draggedFromColumnId = SimpleKId("col-2"),
        )

        assertNotNull(target)
        assertEquals(0, target.index)
    }

    @Test
    fun findDropTarget_belowLastCard_returnsLastIndex() {
        val registry = createRegistry()

        registry.registerColumn(
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(200f, 500f)),
        )
        registry.registerCard(
            cardId = SimpleKId("card-1"),
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(10f, 10f), size = Size(180f, 50f)),
        )
        registry.registerCard(
            cardId = SimpleKId("card-2"),
            columnId = SimpleKId("col-1"),
            index = 1,
            bounds = Rect(offset = Offset(10f, 70f), size = Size(180f, 50f)),
        )

        val target = registry.findDropTarget(
            position = Offset(100f, 400f), // Well below last card
            draggedCardId = SimpleKId("card-3"),
            draggedFromColumnId = SimpleKId("col-2"),
        )

        assertNotNull(target)
        assertEquals(2, target.index) // After both cards
    }

    @Test
    fun findDropTarget_emptyColumn_returnsIndex0() {
        val registry = createRegistry()

        registry.registerColumn(
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(200f, 500f)),
        )

        val target = registry.findDropTarget(
            position = Offset(100f, 100f),
            draggedCardId = SimpleKId("card-1"),
            draggedFromColumnId = SimpleKId("col-2"),
        )

        assertNotNull(target)
        assertEquals(0, target.index)
    }

    @Test
    fun findDropTarget_outsideAllColumns_returnsNull() {
        val registry = createRegistry()

        registry.registerColumn(
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(200f, 500f)),
        )

        val target = registry.findDropTarget(
            position = Offset(500f, 100f), // Outside column
            draggedCardId = SimpleKId("card-1"),
            draggedFromColumnId = SimpleKId("col-1"),
        )

        assertNull(target)
    }

    @Test
    fun findDropTarget_excludesDraggedCard() {
        val registry = createRegistry()

        registry.registerColumn(
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(200f, 500f)),
        )
        registry.registerCard(
            cardId = SimpleKId("card-1"),
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(10f, 10f), size = Size(180f, 50f)),
        )
        registry.registerCard(
            cardId = SimpleKId("card-2"),
            columnId = SimpleKId("col-1"),
            index = 1,
            bounds = Rect(offset = Offset(10f, 70f), size = Size(180f, 50f)),
        )

        // Drag card-1 - it should be excluded from hit testing
        val target = registry.findDropTarget(
            position = Offset(100f, 35f), // On card-1's midpoint
            draggedCardId = SimpleKId("card-1"),
            draggedFromColumnId = SimpleKId("col-1"),
        )

        // Since card-1 is excluded and we're above card-2's midpoint,
        // we should get index 0 (before card-2)
        assertNotNull(target)
        assertEquals(0, target.index)
    }

    // ==================== findColumnAt() Tests ====================

    @Test
    fun findColumnAt_overColumn_returnsColumnId() {
        val registry = createRegistry()

        registry.registerColumn(
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(200f, 500f)),
        )
        registry.registerColumn(
            columnId = SimpleKId("col-2"),
            index = 1,
            bounds = Rect(offset = Offset(220f, 0f), size = Size(200f, 500f)),
        )

        val columnId = registry.findColumnAt(Offset(100f, 100f))

        assertEquals("col-1", columnId?.value)
    }

    @Test
    fun findColumnAt_secondColumn_returnsCorrectId() {
        val registry = createRegistry()

        registry.registerColumn(
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(200f, 500f)),
        )
        registry.registerColumn(
            columnId = SimpleKId("col-2"),
            index = 1,
            bounds = Rect(offset = Offset(220f, 0f), size = Size(200f, 500f)),
        )

        val columnId = registry.findColumnAt(Offset(300f, 100f))

        assertEquals("col-2", columnId?.value)
    }

    @Test
    fun findColumnAt_outsideColumns_returnsNull() {
        val registry = createRegistry()

        registry.registerColumn(
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(200f, 500f)),
        )

        val columnId = registry.findColumnAt(Offset(500f, 100f))

        assertNull(columnId)
    }

    // ==================== getColumnBounds() Tests ====================

    @Test
    fun getColumnBounds_existingColumn_returnsBounds() {
        val registry = createRegistry()
        val expectedBounds = Rect(offset = Offset(0f, 0f), size = Size(200f, 500f))

        registry.registerColumn(
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = expectedBounds,
        )

        val bounds = registry.getColumnBounds(SimpleKId("col-1"))

        assertNotNull(bounds)
        assertEquals(expectedBounds, bounds)
    }

    @Test
    fun getColumnBounds_nonExistentColumn_returnsNull() {
        val registry = createRegistry()

        val bounds = registry.getColumnBounds(SimpleKId("non-existent"))

        assertNull(bounds)
    }

    // ==================== clear() Tests ====================

    @Test
    fun clear_removesAllZones() {
        val registry = createRegistry()

        registry.registerColumn(
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(200f, 500f)),
        )
        registry.registerCard(
            cardId = SimpleKId("card-1"),
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(10f, 10f), size = Size(180f, 50f)),
        )

        registry.clear()

        assertTrue(registry.cardZones.isEmpty())
        assertTrue(registry.columnZones.isEmpty())
    }

    @Test
    fun clear_emptyRegistry_doesNotCrash() {
        val registry = createRegistry()

        registry.clear()

        assertTrue(registry.cardZones.isEmpty())
        assertTrue(registry.columnZones.isEmpty())
    }

    // ==================== Edge Cases ====================

    @Test
    fun findDropTarget_cardsWithGaps_handlesCorrectly() {
        val registry = createRegistry()

        registry.registerColumn(
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(0f, 0f), size = Size(200f, 500f)),
        )
        // Cards with 50px gaps between them
        registry.registerCard(
            cardId = SimpleKId("card-1"),
            columnId = SimpleKId("col-1"),
            index = 0,
            bounds = Rect(offset = Offset(10f, 10f), size = Size(180f, 50f)),
        )
        registry.registerCard(
            cardId = SimpleKId("card-2"),
            columnId = SimpleKId("col-1"),
            index = 1,
            bounds = Rect(offset = Offset(10f, 110f), size = Size(180f, 50f)), // 50px gap
        )

        // Position in the gap between cards
        val target = registry.findDropTarget(
            position = Offset(100f, 80f), // In the gap
            draggedCardId = SimpleKId("card-3"),
            draggedFromColumnId = SimpleKId("col-2"),
        )

        assertNotNull(target)
        // Below card-1's midpoint (35f), so index should be 1 (after card-1)
        assertEquals(1, target.index)
    }
}
