# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2025-01-20

### Added

- **Core SimpleK Board Component**
  - `SimpleKBoard` composable for rendering interactive kanban boards
  - `SimpleKState` for managing board state with unidirectional data flow
  - `rememberSimpleKState` composable function for state creation

- **Drag and Drop**
  - Long-press to drag cards between columns
  - Smooth spring-based animations during card movement
  - Real-time card position updates during drag

- **Zoom-Out Navigation**
  - Automatic zoom-out when dragging cards outside their column
  - Miniature column view for easy cross-column navigation
  - Portrait and landscape layout support in zoom-out mode
  - Auto-scroll when dragging near edges

- **Haptic Feedback**
  - Native haptic feedback on iOS and Android
  - Feedback on drag start, card movement, and drop

- **Theming**
  - `BoardTheme` for main board colors (Light and Dark presets)
  - `OverlayTheme` for zoom-out overlay colors (Light and Dark presets)
  - `SimpleKTypography` for text style customization
  - Configurable corner radius for cards and columns

- **Configuration**
  - `SimpleKConfig` for comprehensive customization
  - Pre-built configurations: Default, Compact, Wide
  - Animation configuration via `SimpleKAnimationConfig`
  - Enable/disable individual features (drag, zoom, haptics, chevrons)

- **DSL Builder**
  - `simpleKBoard { }` DSL for declarative board creation
  - `column()` and `card()` builders
  - `CardLabelsBuilder` for label creation with preset colors

- **Data Models**
  - `SimpleKBoard<T>` - Board with generic card type
  - `SimpleKColumn<T>` - Column with WIP limits support
  - `SimpleKItem` interface for custom card types
  - `SimpleKId` value class for type-safe identifiers
  - `DefaultCard` built-in card type with title, description, labels, priority

- **Pre-built Components**
  - `SimpleCard` - Basic card with title
  - `TrelloCard` - Feature-rich card with labels, priority, and image support

- **State Operations**
  - `moveCard()` - Move cards between columns
  - `addCard()` / `removeCard()` - Card management
  - `addColumn()` / `removeColumn()` - Column management
  - `moveColumn()` - Column reordering
  - Undo/redo support with configurable history size

- **Platform Support**
  - Android (minSdk 24)
  - iOS (arm64, simulatorArm64)

### Dependencies

- Kotlin 2.0+
- Compose Multiplatform 1.7+
- UUID library for identifier generation

[Unreleased]: https://github.com/sigmadeltasoftware/simplek/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/sigmadeltasoftware/simplek/releases/tag/v0.1.0
