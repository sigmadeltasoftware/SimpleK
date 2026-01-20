package io.github.simplek.platform

/**
 * Desktop Platform Support Documentation
 * ======================================
 *
 * The Kankan library supports desktop platforms (Windows, macOS, Linux) through
 * Compose Multiplatform for Desktop. This enables using the Kanban board in
 * desktop applications with full mouse and keyboard support.
 *
 * Desktop-Specific Considerations:
 * --------------------------------
 *
 * 1. Mouse Interaction:
 *    - Click and drag works naturally with mouse input
 *    - Long-press timeout can be reduced for desktop via KanbanConfig
 *    - Hover states are visible on mini column cards
 *
 * 2. Keyboard Navigation:
 *    - Cards have Role.Button semantics enabling Tab navigation
 *    - Chevron buttons are keyboard-accessible
 *    - Arrow key navigation within columns (Tab moves between focusable elements)
 *
 * 3. Haptic Feedback:
 *    - Desktop does not have haptic feedback
 *    - HapticFeedback calls are no-ops on desktop
 *    - Consider adding audio feedback for desktop if needed
 *
 * 4. Window Resizing:
 *    - Board layout adapts to window size changes
 *    - Mini column overlay switches between portrait/landscape based on aspect ratio
 *
 * Desktop Configuration Recommendations:
 * --------------------------------------
 *
 * For desktop apps, consider using configuration like:
 *
 * ```kotlin
 * val desktopConfig = KanbanConfig(
 *     // Shorter long-press for mouse since click-and-hold is faster
 *     longPressDurationMillis = 200L,
 *     // Disable haptics on desktop (no effect anyway)
 *     enableHapticFeedback = false,
 *     // Larger columns for bigger screens
 *     columnWidth = 320.dp,
 * )
 * ```
 *
 * Building for Desktop:
 * ---------------------
 *
 * To build the library for desktop:
 *   ./gradlew :library:desktopJar
 *
 * The desktop JAR includes all dependencies needed for JVM applications.
 *
 * Dependencies for Desktop Consumers:
 * -----------------------------------
 *
 * Add to your desktop app's build.gradle.kts:
 *
 * ```kotlin
 * dependencies {
 *     implementation("be.sigmadelta:kankan-desktop:$version")
 *     // Compose Desktop dependencies
 *     implementation(compose.desktop.currentOs)
 * }
 * ```
 */

// No actual code needed - this file serves as documentation
// All functionality is provided by commonMain code with platform-agnostic APIs
