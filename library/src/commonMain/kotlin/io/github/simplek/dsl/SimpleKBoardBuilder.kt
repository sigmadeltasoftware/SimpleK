package io.github.simplek.dsl

import androidx.compose.ui.graphics.Color
import io.github.simplek.model.CardLabel
import io.github.simplek.model.CardPriority
import io.github.simplek.model.DefaultCard
import io.github.simplek.model.SimpleKBoard
import io.github.simplek.model.SimpleKColumn
import io.github.simplek.model.SimpleKId
import io.github.simplek.model.SimpleKItem

/**
 * DSL for building Kanban boards in a declarative style.
 *
 * Example:
 * ```
 * val board = simpleKBoard {
 *     column("To Do") {
 *         card("Task 1", "Description")
 *         card("Task 2")
 *     }
 *     column("In Progress", maxItems = 3) {
 *         card("Task 3")
 *     }
 *     column("Done")
 * }
 * ```
 */
@DslMarker
annotation class SimpleKDsl

/**
 * Creates a Kanban board using the DSL with DefaultCard type.
 */
fun simpleKBoard(
    id: SimpleKId = SimpleKId.generate(),
    builder: SimpleKBoardBuilder.() -> Unit,
): SimpleKBoard<DefaultCard> {
    return SimpleKBoardBuilder(id).apply(builder).build()
}

/**
 * Creates a Kanban board using the DSL with a generic card type.
 * Use this when you have a custom card type that implements [SimpleKItem].
 *
 * Example:
 * ```
 * data class MyCard(
 *     override val id: SimpleKId,
 *     val name: String,
 *     val customField: Int
 * ) : SimpleKItem
 *
 * val board = buildSimpleKBoard<MyCard> {
 *     column("To Do") {
 *         card(MyCard(SimpleKId.generate(), "Task 1", 42))
 *     }
 * }
 * ```
 */
fun <T : SimpleKItem> buildSimpleKBoard(
    id: SimpleKId = SimpleKId.generate(),
    builder: GenericSimpleKBoardBuilder<T>.() -> Unit,
): SimpleKBoard<T> {
    return GenericSimpleKBoardBuilder<T>(id).apply(builder).build()
}

/**
 * Generic builder for creating a Kanban board with any card type.
 */
@SimpleKDsl
class GenericSimpleKBoardBuilder<T : SimpleKItem>(
    private val id: SimpleKId,
) {
    private val columns = mutableListOf<SimpleKColumn<T>>()

    /**
     * Add a column to the board.
     */
    fun column(
        title: String,
        id: SimpleKId = SimpleKId.generate(),
        color: Color? = null,
        maxItems: Int? = null,
        builder: GenericSimpleKColumnBuilder<T>.() -> Unit = {},
    ) {
        columns.add(
            GenericSimpleKColumnBuilder<T>(id, title, color, maxItems).apply(builder).build()
        )
    }

    internal fun build(): SimpleKBoard<T> {
        return SimpleKBoard(id = id, columns = columns.toList())
    }
}

/**
 * Generic builder for creating a Kanban column with any card type.
 */
@SimpleKDsl
class GenericSimpleKColumnBuilder<T : SimpleKItem>(
    private val id: SimpleKId,
    private val title: String,
    private val color: Color?,
    private val maxItems: Int?,
) {
    private val items = mutableListOf<T>()

    /**
     * Add a card to the column.
     */
    fun card(item: T) {
        items.add(item)
    }

    /**
     * Add multiple cards to the column.
     */
    fun cards(vararg items: T) {
        this.items.addAll(items)
    }

    /**
     * Add multiple cards to the column from a list.
     */
    fun cards(items: List<T>) {
        this.items.addAll(items)
    }

    internal fun build(): SimpleKColumn<T> {
        return SimpleKColumn(
            id = id,
            title = title,
            items = items.toList(),
            color = color,
            maxItems = maxItems,
        )
    }
}

/**
 * Builder for creating a Kanban board.
 */
@SimpleKDsl
class SimpleKBoardBuilder(
    private val id: SimpleKId,
) {
    private val columns = mutableListOf<SimpleKColumn<DefaultCard>>()

    /**
     * Add a column to the board.
     */
    fun column(
        title: String,
        id: SimpleKId = SimpleKId.generate(),
        color: Color? = null,
        maxItems: Int? = null,
        builder: SimpleKColumnBuilder.() -> Unit = {},
    ) {
        columns.add(
            SimpleKColumnBuilder(id, title, color, maxItems).apply(builder).build()
        )
    }

    internal fun build(): SimpleKBoard<DefaultCard> {
        return SimpleKBoard(id = id, columns = columns.toList())
    }
}

/**
 * Builder for creating a Kanban column.
 */
@SimpleKDsl
class SimpleKColumnBuilder(
    private val id: SimpleKId,
    private val title: String,
    private val color: Color?,
    private val maxItems: Int?,
) {
    private val items = mutableListOf<DefaultCard>()

    /**
     * Add a card to the column.
     */
    fun card(
        title: String,
        description: String? = null,
        id: SimpleKId = SimpleKId.generate(),
        priority: CardPriority = CardPriority.NONE,
        labels: List<CardLabel> = emptyList(),
        imageUrl: String? = null,
    ) {
        items.add(
            DefaultCard(
                id = id,
                title = title,
                description = description,
                priority = priority,
                labels = labels,
                imageUrl = imageUrl,
            )
        )
    }

    /**
     * Add labels using a builder.
     */
    fun card(
        title: String,
        description: String? = null,
        id: SimpleKId = SimpleKId.generate(),
        priority: CardPriority = CardPriority.NONE,
        imageUrl: String? = null,
        labelsBuilder: CardLabelsBuilder.() -> Unit,
    ) {
        items.add(
            DefaultCard(
                id = id,
                title = title,
                description = description,
                priority = priority,
                labels = CardLabelsBuilder().apply(labelsBuilder).build(),
                imageUrl = imageUrl,
            )
        )
    }

    internal fun build(): SimpleKColumn<DefaultCard> {
        return SimpleKColumn(
            id = id,
            title = title,
            items = items.toList(),
            color = color,
            maxItems = maxItems,
        )
    }
}

/**
 * Builder for creating card labels.
 */
@SimpleKDsl
class CardLabelsBuilder {
    private val labels = mutableListOf<CardLabel>()

    /**
     * Add a label.
     */
    fun label(
        name: String,
        color: Color,
        id: SimpleKId = SimpleKId.generate(),
    ) {
        labels.add(CardLabel(id = id, name = name, color = color))
    }

    // Predefined label colors
    fun bug(name: String = "Bug") = label(name, Color(0xFFEF4444))
    fun feature(name: String = "Feature") = label(name, Color(0xFF22C55E))
    fun enhancement(name: String = "Enhancement") = label(name, Color(0xFF3B82F6))
    fun documentation(name: String = "Docs") = label(name, Color(0xFF8B5CF6))
    fun urgent(name: String = "Urgent") = label(name, Color(0xFFF59E0B))

    internal fun build(): List<CardLabel> = labels.toList()
}

// Convenience extensions for common label colors
object LabelColors {
    val Red = Color(0xFFEF4444)
    val Orange = Color(0xFFF97316)
    val Yellow = Color(0xFFF59E0B)
    val Green = Color(0xFF22C55E)
    val Teal = Color(0xFF14B8A6)
    val Blue = Color(0xFF3B82F6)
    val Indigo = Color(0xFF6366F1)
    val Purple = Color(0xFF8B5CF6)
    val Pink = Color(0xFFEC4899)
    val Gray = Color(0xFF6B7280)
}

// Convenience extensions for column colors
object ColumnColors {
    val Todo = Color(0xFF6B7280)
    val InProgress = Color(0xFF3B82F6)
    val Review = Color(0xFFF59E0B)
    val Done = Color(0xFF22C55E)
    val Blocked = Color(0xFFEF4444)
}
