# Module SimpleK

A smooth, animated Kanban board library for Compose Multiplatform.

## Features

- Drag & drop cards between columns
- Zoom-out navigation for quick cross-column moves
- Smooth spring-based animations
- Haptic feedback on iOS and Android
- Light and dark themes
- Customizable card content via composable slots

## Getting Started

Create a board using the DSL:

```kotlin
val board = simpleKBoard {
    column("To Do") {
        card("Task 1", "Description")
        card("Task 2")
    }
    column("In Progress", maxItems = 3) {
        card("Task 3")
    }
    column("Done")
}
```

Then render it:

```kotlin
val state = rememberSimpleKState(board)

SimpleKBoard(
    state = state,
    cardContent = { card ->
        Text(card.title)
    }
)
```

## Package Structure

| Package | Description |
|---------|-------------|
| `io.github.simplek` | Main composables and entry points |
| `io.github.simplek.config` | Configuration and theming |
| `io.github.simplek.state` | State management |
| `io.github.simplek.model` | Data models |
| `io.github.simplek.dsl` | DSL builders |
| `io.github.simplek.scope` | Composable scopes |
| `io.github.simplek.components` | Pre-built card components |
