package io.github.simplek.config

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Configuration for Kanban board behavior and appearance.
 *
 * This class controls all aspects of the board's visual appearance and interaction behavior.
 * Use the predefined configurations ([Default], [Compact], [Wide]) or create custom instances.
 *
 * ## Layout Properties
 *
 * - [columnWidth]: Width of each column
 * - [columnSpacing]: Horizontal spacing between columns
 * - [cardSpacing]: Vertical spacing between cards within a column
 * - [columnPadding]: Internal padding of each column
 * - [boardPadding]: Padding around the entire board
 *
 * ## Interaction Settings
 *
 * - [enableCardDrag]: Enable/disable card drag-and-drop
 * - [enableColumnReorder]: Enable/disable column reordering
 * - [enableZoomOutDrag]: Enable zoom-out view when dragging cards across columns
 * - [enableHapticFeedback]: Enable native haptic feedback during drag operations
 * - [showChevronButtons]: Show quick-move buttons on cards
 * - [enforceWipLimits]: When true, prevents cards from being placed in columns at their WIP limit
 *
 * ## Zoom-Out Configuration
 *
 * - [zoomOutDurationMillis]: Animation duration when zooming out
 * - [zoomInDurationMillis]: Animation duration when zooming back in
 * - [miniatureColumnWidth]: Width of mini column cards in zoom-out view
 * - [miniatureColumnHeight]: Height of mini column cards in zoom-out view
 *
 * ## Theming
 *
 * - [boardTheme]: Colors for the main board (columns, cards, accents)
 * - [overlayTheme]: Colors for the zoom-out overlay
 * - [typography]: Text styles for cards and headers
 * - [cardCornerRadius]: Corner radius for card shapes
 * - [columnCornerRadius]: Corner radius for column shapes
 *
 * ## Example
 *
 * ```kotlin
 * val config = SimpleKConfig(
 *     columnWidth = 300.dp,
 *     enableZoomOutDrag = true,
 *     boardTheme = BoardTheme.Dark,
 *     overlayTheme = OverlayTheme.Dark,
 * )
 *
 * SimpleKBoard(
 *     state = state,
 *     config = config,
 *     cardContent = { /* ... */ }
 * )
 * ```
 *
 * @see BoardTheme
 * @see OverlayTheme
 * @see SimpleKAnimationConfig
 */
@Immutable
data class SimpleKConfig(
    val columnWidth: Dp = 280.dp,
    val columnSpacing: Dp = 12.dp,
    val cardSpacing: Dp = 8.dp,
    val columnPadding: PaddingValues = PaddingValues(8.dp),
    val boardPadding: PaddingValues = PaddingValues(16.dp),
    val enableColumnReorder: Boolean = true,
    val enableCardDrag: Boolean = true,
    val scrollEdgeThreshold: Dp = 48.dp,
    val scrollSpeed: Float = 800f,
    val longPressDurationMillis: Long = 300L,
    val animationConfig: SimpleKAnimationConfig = SimpleKAnimationConfig.Default,
    // Zoom-out drag configuration
    val enableZoomOutDrag: Boolean = true,
    val zoomOutDurationMillis: Long = 800L,
    val zoomInDurationMillis: Long = 400L,
    val miniatureColumnWidth: Dp = 72.dp,
    val miniatureColumnHeight: Dp = 96.dp,
    val miniatureColumnSpacing: Dp = 24.dp,
    val overlayTheme: OverlayTheme = OverlayTheme.Light,
    val boardTheme: BoardTheme = BoardTheme.Light,
    // Haptic feedback
    val enableHapticFeedback: Boolean = true,
    // Shape configuration
    val cardCornerRadius: Dp = 8.dp,
    val columnCornerRadius: Dp = 12.dp,
    // Typography configuration
    val typography: SimpleKTypography = SimpleKTypography.Default,
    // Drag overlay configuration
    val dragOverlayScale: Float = 1.05f,
    val scrollEdgeZonePercentage: Float = 0.20f,
    // Chevron button visibility
    val showChevronButtons: Boolean = true,
    // Undo/redo history configuration
    val maxHistorySize: Int = 50,
    // Column collapse configuration
    val enableColumnCollapse: Boolean = true,
    val collapsedColumnWidth: Dp = 48.dp,
    // Overlay hint text (for i18n support)
    val overlayHintText: String = "Drag to a column to move card",
    // WIP limit enforcement - when true, prevents cards from being placed in full columns
    val enforceWipLimits: Boolean = false,
) {
    companion object {
        val Default = SimpleKConfig()

        val Compact = SimpleKConfig(
            columnWidth = 240.dp,
            columnSpacing = 8.dp,
            cardSpacing = 6.dp,
            columnPadding = PaddingValues(6.dp),
            cardCornerRadius = 6.dp,
            columnCornerRadius = 8.dp,
        )

        val Wide = SimpleKConfig(
            columnWidth = 320.dp,
            columnSpacing = 16.dp,
            cardSpacing = 10.dp,
            cardCornerRadius = 10.dp,
            columnCornerRadius = 14.dp,
        )
    }
}

/**
 * Typography configuration for Kanban board elements.
 */
@Immutable
data class SimpleKTypography(
    val cardTitle: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
    ),
    val cardSubtitle: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
    ),
    val columnHeader: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
    ),
) {
    companion object {
        val Default = SimpleKTypography()

        val Compact = SimpleKTypography(
            cardTitle = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
            cardSubtitle = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
            ),
            columnHeader = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )

        val Large = SimpleKTypography(
            cardTitle = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
            cardSubtitle = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
            ),
            columnHeader = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/**
 * Animation configuration for the Kanban board.
 */
@Immutable
data class SimpleKAnimationConfig(
    val cardPickupSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    ),
    val cardDropSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    ),
    val placeholderSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
    ),
    val reorderSpec: AnimationSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    ),
    val elevationSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh,
    ),
) {
    companion object {
        val Default = SimpleKAnimationConfig()

        val Snappy = SimpleKAnimationConfig(
            cardPickupSpec = spring(stiffness = Spring.StiffnessHigh),
            cardDropSpec = spring(stiffness = Spring.StiffnessHigh),
        )

        val Smooth = SimpleKAnimationConfig(
            cardPickupSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow,
            ),
            cardDropSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }
}

/**
 * Theme configuration for the main Kanban board.
 *
 * Controls colors for columns, cards, and interactive elements displayed
 * in the normal (non-zoomed) board view.
 *
 * ## Pre-built Themes
 *
 * - [Light]: Clean whites and grays with indigo accents
 * - [Dark]: Deep grays with indigo/purple accents
 *
 * ## Custom Theme Example
 *
 * ```kotlin
 * val customTheme = BoardTheme(
 *     columnBackground = Color(0xFFF0F4F8),
 *     columnBackgroundDragTarget = Color(0xFF3B82F6),
 *     cardBackground = Color.White,
 *     chevronColor = Color(0xFF64748B),
 *     accentColor = Color(0xFF3B82F6),
 * )
 * ```
 *
 * @property columnBackground Background color for column containers
 * @property columnBackgroundDragTarget Highlight color when a column is a drag target
 * @property cardBackground Background color for the floating card during drag
 * @property chevronColor Color for the quick-move chevron buttons
 * @property accentColor Primary accent color used for highlights and indicators
 */
@Immutable
data class BoardTheme(
    // Column colors
    val columnBackground: Color,
    val columnBackgroundDragTarget: Color,

    // Card colors (for drag overlay)
    val cardBackground: Color,

    // Interactive elements
    val chevronColor: Color,
    val accentColor: Color,
) {
    companion object {
        /**
         * Light theme - clean whites and grays.
         */
        val Light = BoardTheme(
            columnBackground = Color(0xFFF5F5F5),
            columnBackgroundDragTarget = Color(0xFF6366F1),
            cardBackground = Color.White,
            chevronColor = Color(0xFF9CA3AF),
            accentColor = Color(0xFF6366F1),
        )

        /**
         * Dark theme - deep grays with purple accents.
         */
        val Dark = BoardTheme(
            columnBackground = Color(0xFF1F2937),
            columnBackgroundDragTarget = Color(0xFF818CF8),
            cardBackground = Color(0xFF374151),
            chevronColor = Color(0xFF6B7280),
            accentColor = Color(0xFF818CF8),
        )
    }
}

/**
 * Theme configuration for the zoom-out overlay.
 *
 * Controls colors for the background scrim, mini column cards, scroll indicators,
 * and hint text displayed during the zoom-out drag navigation.
 *
 * ## Pre-built Themes
 *
 * - [Light]: Semi-transparent white scrim with clean card styling
 * - [Dark]: Semi-transparent dark scrim with depth and subtle highlights
 *
 * ## Key Properties
 *
 * **Background**
 * - [scrimColor] and [scrimAlpha]: Semi-transparent background overlay
 *
 * **Mini Column Cards**
 * - [cardBackground]: Normal card background
 * - [cardBackgroundSource]: Background for the source column (where card came from)
 * - [cardBorder] / [cardBorderHovered]: Border colors for normal and hover states
 * - [glowColor]: Glow effect color when hovering over a column
 *
 * **Text**
 * - [titleColor] / [titleColorSource]: Column title colors
 * - [countColor] / [countColorHovered]: Card count badge colors
 *
 * **Scroll Indicators**
 * - [edgeGradientIdle] / [edgeGradientActive]: Edge gradient when scrollable
 * - [edgeArrowColor]: Arrow indicator color
 *
 * @see SimpleKConfig.overlayTheme
 */
@Immutable
data class OverlayTheme(
    // Background
    val scrimColor: Color,
    val scrimAlpha: Float,

    // Mini column cards
    val cardBackground: Color,
    val cardBackgroundSource: Color,
    val cardBorder: Color,
    val cardBorderHovered: Color,

    // Text colors
    val titleColor: Color,
    val titleColorSource: Color,
    val countColor: Color,
    val countColorHovered: Color,
    val countBackground: Color,
    val countBackgroundHovered: Color,

    // Glow effect
    val glowColor: Color,

    // Scroll edge indicators
    val edgeGradientIdle: Color,
    val edgeGradientActive: Color,
    val edgeArrowColor: Color,

    // Hint text
    val hintTextColor: Color,

    // Disabled state (for WIP limit enforcement)
    val cardBackgroundDisabled: Color,
    val cardBorderDisabled: Color,
    val titleColorDisabled: Color,
    val countColorDisabled: Color,
    val countBackgroundDisabled: Color,
) {
    companion object {
        /**
         * Subtle light theme with soft shadows and gentle colors.
         * Works well on light app backgrounds.
         */
        val Light = OverlayTheme(
            // Semi-transparent white scrim - lets content show through
            scrimColor = Color.White,
            scrimAlpha = 0.65f,

            // Cards - clean white with subtle shadows
            cardBackground = Color.White,
            cardBackgroundSource = Color(0xFFF5F5F5),
            cardBorder = Color(0xFFE0E0E0),
            cardBorderHovered = Color(0xFF6366F1),

            // Text - readable dark grays
            titleColor = Color(0xFF374151),
            titleColorSource = Color(0xFF9CA3AF),
            countColor = Color(0xFF6B7280),
            countColorHovered = Color(0xFF6366F1),
            countBackground = Color(0xFFF3F4F6),
            countBackgroundHovered = Color(0xFFEEF2FF),

            // Subtle purple glow
            glowColor = Color(0xFF6366F1),

            // Edge indicators - solid colors visible against transparent scrim
            edgeGradientIdle = Color(0xFFE5E7EB),
            edgeGradientActive = Color(0xFFC7D2FE),
            edgeArrowColor = Color(0xFF6366F1),

            // Hint text
            hintTextColor = Color(0xFF6B7280),

            // Disabled state - muted colors for columns at WIP limit
            cardBackgroundDisabled = Color(0xFFF9FAFB),
            cardBorderDisabled = Color(0xFFE5E7EB),
            titleColorDisabled = Color(0xFFD1D5DB),
            countColorDisabled = Color(0xFFD1D5DB),
            countBackgroundDisabled = Color(0xFFF3F4F6),
        )

        /**
         * Elegant dark theme with depth and subtle highlights.
         * Works well on dark app backgrounds.
         */
        val Dark = OverlayTheme(
            // Semi-transparent dark scrim - lets content show through
            scrimColor = Color(0xFF111827),
            scrimAlpha = 0.75f,

            // Cards - dark with subtle transparency
            cardBackground = Color(0xFF1F2937),
            cardBackgroundSource = Color(0xFF111827),
            cardBorder = Color(0xFF374151),
            cardBorderHovered = Color(0xFF818CF8),

            // Text - soft whites and grays
            titleColor = Color(0xFFF9FAFB),
            titleColorSource = Color(0xFF6B7280),
            countColor = Color(0xFFD1D5DB),
            countColorHovered = Color(0xFF818CF8),
            countBackground = Color(0xFF374151),
            countBackgroundHovered = Color(0xFF312E81),

            // Indigo glow
            glowColor = Color(0xFF818CF8),

            // Edge indicators - solid colors visible against transparent scrim
            edgeGradientIdle = Color(0xFF1F2937),
            edgeGradientActive = Color(0xFF4338CA),
            edgeArrowColor = Color(0xFFA5B4FC),

            // Hint text
            hintTextColor = Color(0xFF9CA3AF),

            // Disabled state - muted colors for columns at WIP limit
            cardBackgroundDisabled = Color(0xFF111827),
            cardBorderDisabled = Color(0xFF1F2937),
            titleColorDisabled = Color(0xFF4B5563),
            countColorDisabled = Color(0xFF4B5563),
            countBackgroundDisabled = Color(0xFF1F2937),
        )
    }
}
