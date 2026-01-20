package io.github.simplek

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import io.github.simplek.config.BoardTheme
import io.github.simplek.config.SimpleKConfig
import io.github.simplek.config.SimpleKTypography
import io.github.simplek.config.OverlayTheme
import io.github.simplek.internal.AutoScrollController
import io.github.simplek.internal.DropZoneRegistry
import io.github.simplek.model.SimpleKColumn
import io.github.simplek.model.SimpleKId
import io.github.simplek.model.SimpleKItem
import io.github.simplek.scope.SimpleKCardScope
import io.github.simplek.scope.SimpleKCardScopeImpl
import io.github.simplek.scope.SimpleKHeaderScope
import io.github.simplek.scope.SimpleKHeaderScopeImpl
import io.github.simplek.state.DragState
import io.github.simplek.state.SimpleKState
import io.github.simplek.state.ZoomPhase
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import kotlin.time.TimeSource

// CompositionLocals for internal use
internal val LocalSimpleKState = compositionLocalOf<SimpleKState<*>?> { null }
internal val LocalDropZoneRegistry = compositionLocalOf<DropZoneRegistry?> { null }
internal val LocalAutoScrollController = compositionLocalOf<AutoScrollController?> { null }

/**
 * CompositionLocal for the current board theme.
 * Pre-built card components use this to access theme colors.
 */
val LocalBoardTheme = compositionLocalOf { BoardTheme.Light }

/**
 * CompositionLocal for the current typography configuration.
 * Pre-built card components use this to access text styles.
 */
val LocalSimpleKTypography = compositionLocalOf { SimpleKTypography.Default }

/**
 * CompositionLocal for the card corner radius.
 * Pre-built card components use this for consistent corner styling.
 */
val LocalCardCornerRadius = compositionLocalOf { 8.dp }

// Note: Mini column dimensions are now in SimpleKConfig (miniatureColumnWidth/Height)

/**
 * Main Kanban board composable that renders an interactive drag-and-drop board.
 *
 * The board supports:
 * - **Card drag-and-drop**: Long-press on a card to pick it up and drag it to a new position
 * - **Zoom-out navigation**: When dragging a card outside its column, the board zooms out to show
 *   miniature column representations for easy cross-column navigation
 * - **Smooth animations**: All movements are animated with spring physics
 * - **Haptic feedback**: Native haptic feedback on drag start and card movements
 *
 * ## Basic Usage
 *
 * ```kotlin
 * val board = simpleKBoard {
 *     column("To Do") {
 *         card("Task 1", "Description")
 *         card("Task 2")
 *     }
 *     column("Done")
 * }
 * val state = rememberSimpleKState(board)
 *
 * SimpleKBoard(
 *     state = state,
 *     cardContent = { card ->
 *         Text(card.title)
 *     }
 * )
 * ```
 *
 * ## Customization
 *
 * Use [SimpleKConfig] to customize appearance and behavior:
 * - Column width, spacing, and padding
 * - Card spacing
 * - Enable/disable drag features
 * - Animation durations
 * - Light/dark themes
 *
 * @param T The type of items in the board, must implement [SimpleKItem]
 * @param state The board state holder, created with [rememberSimpleKState]
 * @param modifier Modifier for the board container
 * @param cardContent Composable slot for rendering each card. Receives a [SimpleKCardScope]
 *   that provides drag state information and a drag handle modifier
 * @param columnHeader Optional composable slot for custom column headers. Receives a
 *   [SimpleKHeaderScope] with item count and WIP limit information
 * @param columnFooter Optional composable slot for column footers (e.g., "Add card" button)
 * @param emptyColumnContent Optional composable content shown when a column has no cards
 * @param config Board configuration options including dimensions, animations, and themes
 * @param callbacks Event callbacks for card interactions (moved, clicked, drag start/end)
 *
 * @see SimpleKState
 * @see SimpleKConfig
 * @see SimpleKCallbacks
 * @see rememberSimpleKState
 */
@Composable
fun <T : SimpleKItem> SimpleKBoard(
    state: SimpleKState<T>,
    modifier: Modifier = Modifier,
    cardContent: @Composable SimpleKCardScope.(card: T) -> Unit,
    columnHeader: (@Composable SimpleKHeaderScope.(column: SimpleKColumn<T>) -> Unit)? = null,
    columnFooter: (@Composable (column: SimpleKColumn<T>) -> Unit)? = null,
    emptyColumnContent: (@Composable (column: SimpleKColumn<T>) -> Unit)? = null,
    config: SimpleKConfig = SimpleKConfig.Default,
    callbacks: SimpleKCallbacks<T> = SimpleKCallbacks(),
) {
    // Extract callbacks for use in composables
    val onCardMoved = callbacks.onCardMoved
    val onDragStart = callbacks.onDragStart
    val onDragEnd = callbacks.onDragEnd
    val onCardClick = callbacks.onCardClick
    val canMoveCard = callbacks.canMoveCard
    val coroutineScope = rememberCoroutineScope()
    val dropZoneRegistry = remember { DropZoneRegistry() }
    val autoScrollController = remember { AutoScrollController(coroutineScope) }
    val boardScrollState = rememberScrollState()
    val hapticFeedback = LocalHapticFeedback.current

    // Track the board's position for coordinate calculations
    var boardOffset by remember { mutableStateOf(Offset.Zero) }

    // Check if currently dragging a card
    val isDraggingCard = state.cardDragState !is DragState.Idle

    // Check if scroll should be disabled (pointer down on card OR dragging)
    val shouldDisableScroll by state.shouldDisableScroll

    // Get zoom progress for animated transitions
    val zoomProgress = state.zoomProgress
    val isZooming = state.zoomPhase != ZoomPhase.Normal

    CompositionLocalProvider(
        LocalSimpleKState provides state,
        LocalDropZoneRegistry provides dropZoneRegistry,
        LocalAutoScrollController provides autoScrollController,
        LocalBoardTheme provides config.boardTheme,
        LocalSimpleKTypography provides config.typography,
        LocalCardCornerRadius provides config.cardCornerRadius,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    boardOffset = coordinates.positionInRoot()
                }
        ) {
            // Normal column layout - always rendered to keep gesture handlers alive
            // Fades out during zoom-out transition
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(
                        state = boardScrollState,
                        enabled = !shouldDisableScroll && !isZooming
                    )
                    .graphicsLayer {
                        // Fade out normal columns as we zoom out
                        alpha = lerp(1f, 0f, zoomProgress)
                    }
                    .padding(config.boardPadding),
                horizontalArrangement = Arrangement.spacedBy(config.columnSpacing),
            ) {
                state.board.columns.forEachIndexed { columnIndex, column ->
                    SimpleKColumnContent(
                        state = state,
                        column = column,
                        columnIndex = columnIndex,
                        config = config,
                        dropZoneRegistry = dropZoneRegistry,
                        autoScrollController = autoScrollController,
                        boardOffset = boardOffset,
                        cardContent = cardContent,
                        columnHeader = columnHeader,
                        columnFooter = columnFooter,
                        emptyColumnContent = emptyColumnContent,
                        zoomProgress = zoomProgress,
                        hapticFeedback = hapticFeedback,
                        onCardMoved = onCardMoved,
                        onDragStart = onDragStart,
                        onDragEnd = onDragEnd,
                        onCardClick = onCardClick,
                        canMoveCard = canMoveCard,
                    )
                }
            }

            // Mini column layout overlay - shown during drag with zoom
            // Only appears once zoom has progressed enough (cards have started fading)
            // This creates a cleaner transition where cards zoom out first, then miniatures appear
            val miniatureAlpha = if (zoomProgress > 0.3f) {
                // Remap 0.3-1.0 to 0-1 for fade in
                ((zoomProgress - 0.3f) / 0.7f).coerceIn(0f, 1f)
            } else {
                0f
            }

            if ((isDraggingCard || isZooming) && miniatureAlpha > 0f) {
                MiniColumnLayout(
                    state = state,
                    config = config,
                    theme = config.overlayTheme,
                    hapticFeedback = hapticFeedback,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = miniatureAlpha
                        },
                )
            }

            // Drag overlay - floating card
            DragOverlay(
                state = state,
                config = config,
                boardOffset = boardOffset,
                cardContent = cardContent,
            )
        }
    }
}

/**
 * Mini column layout shown during card drag.
 * Columns shrink to mini representations centered on screen.
 * Auto-scrolls when dragging near edges since user is already in a drag gesture.
 * In portrait mode (height > width), columns are arranged vertically to maximize screen space.
 */
@Composable
private fun <T : SimpleKItem> MiniColumnLayout(
    state: SimpleKState<T>,
    config: SimpleKConfig,
    theme: OverlayTheme,
    hapticFeedback: HapticFeedback,
    modifier: Modifier = Modifier,
) {
    val columns = state.board.columns
    val scrollState = rememberScrollState()

    // Get source column from drag state
    val sourceColumnId = when (val dragState = state.cardDragState) {
        is DragState.Dragging -> dragState.sourceColumnId
        is DragState.OverDropTarget -> dragState.sourceColumnId
        else -> null
    }

    // Track container bounds for edge detection
    var containerBounds by remember { mutableStateOf(Rect.Zero) }

    // Get current drag position for hit testing
    val dragPosition = when (val dragState = state.cardDragState) {
        is DragState.Dragging -> dragState.currentOffset
        is DragState.OverDropTarget -> dragState.currentOffset
        else -> null
    }

    // Track mini column bounds for hit detection
    val miniColumnBounds = remember { mutableMapOf<SimpleKId, Rect>() }

    // Clear miniColumnBounds when overlay is dismissed to prevent memory leak
    DisposableEffect(Unit) {
        onDispose {
            miniColumnBounds.clear()
        }
    }

    // Track orientation for scroll logic
    var isPortrait by remember { mutableStateOf(false) }

    // Scroll to center source column when overlay appears
    LaunchedEffect(sourceColumnId, containerBounds, isPortrait) {
        if (sourceColumnId != null && columns.isNotEmpty() && containerBounds != Rect.Zero) {
            val sourceIndex = columns.indexOfFirst { it.id == sourceColumnId }
            if (sourceIndex >= 0) {
                // Wait for layout to stabilize
                kotlinx.coroutines.delay(50)

                // If content doesn't overflow, no scrolling needed
                if (scrollState.maxValue <= 0) return@LaunchedEffect

                // For accurate centering, use the captured bounds
                val targetBounds = miniColumnBounds[sourceColumnId]
                if (targetBounds != null) {
                    // Calculate scroll needed to center this column (use y for portrait, x for landscape)
                    val columnCenter = if (isPortrait) targetBounds.center.y else targetBounds.center.x
                    val viewportCenter = if (isPortrait) containerBounds.height / 2 else containerBounds.width / 2
                    val containerStart = if (isPortrait) containerBounds.top else containerBounds.left

                    // Target scroll brings column center to viewport center
                    val targetScroll = (scrollState.value + (columnCenter - containerStart - viewportCenter)).toInt()
                    scrollState.scrollTo(targetScroll.coerceIn(0, scrollState.maxValue))
                } else {
                    // Fallback to fraction-based scrolling
                    val scrollFraction = if (columns.size > 1) {
                        sourceIndex.toFloat() / (columns.size - 1)
                    } else {
                        0f
                    }
                    val targetScroll = (scrollState.maxValue * scrollFraction).toInt()
                    scrollState.scrollTo(targetScroll)
                }
            }
        }
    }

    // Auto-scroll when dragging near edges - 20% from each edge
    val scrollSpeed = config.scrollSpeed

    // Continuous auto-scroll effect - reads all values directly in the loop
    LaunchedEffect(state, isPortrait) {
        while (true) {
            // Read drag position fresh each frame from state
            val pos = when (val dragState = state.cardDragState) {
                is DragState.Dragging -> dragState.currentOffset
                is DragState.OverDropTarget -> dragState.currentOffset
                else -> null
            }

            // If not dragging, use longer delay to save CPU cycles
            if (pos == null) {
                kotlinx.coroutines.delay(100)
                continue
            }

            val bounds = containerBounds
            if (bounds != Rect.Zero) {
                // Calculate threshold fresh each frame based on current bounds
                // Use height for portrait, width for landscape
                val edgeSize = if (isPortrait) bounds.height else bounds.width
                val threshold = edgeSize * config.scrollEdgeZonePercentage

                // Calculate distance from edges based on orientation
                val distanceFromStart = if (isPortrait) pos.y - bounds.top else pos.x - bounds.left
                val distanceFromEnd = if (isPortrait) bounds.bottom - pos.y else bounds.right - pos.x

                when {
                    // Near start edge - scroll backward
                    distanceFromStart < threshold && scrollState.value > 0 -> {
                        val intensity = 1f - (distanceFromStart / threshold).coerceIn(0f, 1f)
                        val scrollAmount = (scrollSpeed * intensity * 0.016f).toInt() // ~60fps
                        scrollState.scrollTo((scrollState.value - scrollAmount).coerceAtLeast(0))
                    }
                    // Near end edge - scroll forward
                    distanceFromEnd < threshold && scrollState.value < scrollState.maxValue -> {
                        val intensity = 1f - (distanceFromEnd / threshold).coerceIn(0f, 1f)
                        val scrollAmount = (scrollSpeed * intensity * 0.016f).toInt()
                        scrollState.scrollTo((scrollState.value + scrollAmount).coerceAtMost(scrollState.maxValue))
                    }
                }
            }
            kotlinx.coroutines.delay(16) // ~60fps
        }
    }

    // Perform hit detection when drag position changes
    // Use hysteresis to prevent jitter at boundaries
    // Skip columns that are disabled (at WIP limit when enforcement is enabled)
    LaunchedEffect(dragPosition) {
        if (dragPosition != null) {
            val currentHovered = state.hoveredOverlayColumnId
            var foundHovered: SimpleKId? = null

            for ((columnId, bounds) in miniColumnBounds) {
                if (bounds.contains(dragPosition)) {
                    // Check if this column is disabled (at WIP limit and enforcement enabled)
                    val column = columns.find { it.id == columnId }
                    val isSource = isSourceColumn(state.cardDragState, columnId)
                    val isDisabled = config.enforceWipLimits && column?.isAtLimit == true && !isSource

                    // Skip disabled columns in hit detection
                    if (!isDisabled) {
                        foundHovered = columnId
                        break
                    }
                }
            }

            // If we found a new column, switch to it
            // If we didn't find any, only unhover if we're clearly outside (with margin)
            if (foundHovered != null) {
                if (foundHovered != currentHovered) {
                    // Haptic feedback when hovering over a new column
                    if (config.enableHapticFeedback) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    state.setHoveredOverlayColumn(foundHovered)
                }
            } else if (currentHovered != null) {
                // Check if we're still near the currently hovered column (within margin)
                val currentBounds = miniColumnBounds[currentHovered]
                val margin = 16f // pixels of margin before unhover
                val expandedBounds = currentBounds?.let {
                    Rect(
                        left = it.left - margin,
                        top = it.top - margin,
                        right = it.right + margin,
                        bottom = it.bottom + margin
                    )
                }
                if (expandedBounds == null || !expandedBounds.contains(dragPosition)) {
                    state.setHoveredOverlayColumn(null)
                }
            }
        }
    }

    // Check if content is scrollable and current scroll position
    val canScrollBackward = scrollState.value > 0
    val canScrollForward = scrollState.value < scrollState.maxValue

    BoxWithConstraints(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                val size = coordinates.size.toSize()
                containerBounds = Rect(offset = position, size = size)
            }
            .background(theme.scrimColor.copy(alpha = theme.scrimAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        // Detect orientation: portrait when height > width
        val portraitMode = maxHeight > maxWidth

        // Update tracked orientation for scroll logic
        LaunchedEffect(portraitMode) {
            isPortrait = portraitMode
        }

        if (portraitMode) {
            // Portrait mode: vertical column layout
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(config.miniatureColumnSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Start spacer - allows first column to be centered when scrolled up
                Spacer(modifier = Modifier.height(32.dp))

                columns.forEach { column ->
                    // Column is disabled if at WIP limit and enforcement is enabled (and not source)
                    val isSource = isSourceColumn(state.cardDragState, column.id)
                    val isDisabled = config.enforceWipLimits && column.isAtLimit && !isSource

                    MiniColumnCard(
                        column = column,
                        isHovered = state.hoveredOverlayColumnId == column.id,
                        isSourceColumn = isSource,
                        isDisabled = isDisabled,
                        theme = theme,
                        onBoundsChanged = { bounds ->
                            miniColumnBounds[column.id] = bounds
                        },
                        miniWidth = config.miniatureColumnWidth,
                        miniHeight = config.miniatureColumnHeight,
                    )
                }

                // End spacer - allows last column to scroll fully into view
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Top edge indicator - shows when more content above
            if (canScrollBackward) {
                val edgeThreshold = containerBounds.height * config.scrollEdgeZonePercentage
                val isNearTopEdge = dragPosition?.let {
                    (it.y - containerBounds.top) < edgeThreshold
                } ?: false

                val topEdgeActivation by animateFloatAsState(
                    targetValue = if (isNearTopEdge) 1f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "top_edge_activation"
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(config.scrollEdgeZonePercentage)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    colorLerp(theme.edgeGradientIdle, theme.edgeGradientActive, topEdgeActivation),
                                    colorLerp(theme.edgeGradientIdle.copy(alpha = 0.6f), theme.edgeGradientActive.copy(alpha = 0.5f), topEdgeActivation),
                                    Color.Transparent,
                                )
                            )
                        )
                ) {
                    if (topEdgeActivation > 0f) {
                        EdgeArrowIndicator(
                            direction = ArrowDirection.UP,
                            color = theme.edgeArrowColor.copy(alpha = 0.9f * topEdgeActivation),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                        )
                    }
                }
            }

            // Bottom edge indicator - shows when more content below
            if (canScrollForward) {
                val edgeThreshold = containerBounds.height * config.scrollEdgeZonePercentage
                val isNearBottomEdge = dragPosition?.let {
                    (containerBounds.bottom - it.y) < edgeThreshold
                } ?: false

                val bottomEdgeActivation by animateFloatAsState(
                    targetValue = if (isNearBottomEdge) 1f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "bottom_edge_activation"
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(config.scrollEdgeZonePercentage)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    colorLerp(theme.edgeGradientIdle.copy(alpha = 0.6f), theme.edgeGradientActive.copy(alpha = 0.5f), bottomEdgeActivation),
                                    colorLerp(theme.edgeGradientIdle, theme.edgeGradientActive, bottomEdgeActivation),
                                )
                            )
                        )
                ) {
                    if (bottomEdgeActivation > 0f) {
                        EdgeArrowIndicator(
                            direction = ArrowDirection.DOWN,
                            color = theme.edgeArrowColor.copy(alpha = 0.9f * bottomEdgeActivation),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                        )
                    }
                }
            }
        } else {
            // Landscape mode: horizontal row layout (original behavior)
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(config.miniatureColumnSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Start spacer - allows first column to be centered when scrolled left
                Spacer(modifier = Modifier.width(32.dp))

                columns.forEach { column ->
                    // Column is disabled if at WIP limit and enforcement is enabled (and not source)
                    val isSource = isSourceColumn(state.cardDragState, column.id)
                    val isDisabled = config.enforceWipLimits && column.isAtLimit && !isSource

                    MiniColumnCard(
                        column = column,
                        isHovered = state.hoveredOverlayColumnId == column.id,
                        isSourceColumn = isSource,
                        isDisabled = isDisabled,
                        theme = theme,
                        onBoundsChanged = { bounds ->
                            miniColumnBounds[column.id] = bounds
                        },
                        miniWidth = config.miniatureColumnWidth,
                        miniHeight = config.miniatureColumnHeight,
                    )
                }

                // End spacer - allows last column to scroll fully into view
                Spacer(modifier = Modifier.width(32.dp))
            }

            // Left edge indicator - shows when more content & highlights scroll zone
            if (canScrollBackward) {
                val edgeThreshold = containerBounds.width * config.scrollEdgeZonePercentage
                val isNearLeftEdge = dragPosition?.let {
                    (it.x - containerBounds.left) < edgeThreshold
                } ?: false

                val leftEdgeActivation by animateFloatAsState(
                    targetValue = if (isNearLeftEdge) 1f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "left_edge_activation"
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .fillMaxWidth(config.scrollEdgeZonePercentage)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    colorLerp(theme.edgeGradientIdle, theme.edgeGradientActive, leftEdgeActivation),
                                    colorLerp(theme.edgeGradientIdle.copy(alpha = 0.6f), theme.edgeGradientActive.copy(alpha = 0.5f), leftEdgeActivation),
                                    Color.Transparent,
                                )
                            )
                        )
                ) {
                    if (leftEdgeActivation > 0f) {
                        EdgeArrowIndicator(
                            direction = ArrowDirection.LEFT,
                            color = theme.edgeArrowColor.copy(alpha = 0.9f * leftEdgeActivation),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 16.dp)
                        )
                    }
                }
            }

            // Right edge indicator - shows when more content & highlights scroll zone
            if (canScrollForward) {
                val edgeThreshold = containerBounds.width * config.scrollEdgeZonePercentage
                val isNearRightEdge = dragPosition?.let {
                    (containerBounds.right - it.x) < edgeThreshold
                } ?: false

                val rightEdgeActivation by animateFloatAsState(
                    targetValue = if (isNearRightEdge) 1f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "right_edge_activation"
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .fillMaxWidth(config.scrollEdgeZonePercentage)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    colorLerp(theme.edgeGradientIdle.copy(alpha = 0.6f), theme.edgeGradientActive.copy(alpha = 0.5f), rightEdgeActivation),
                                    colorLerp(theme.edgeGradientIdle, theme.edgeGradientActive, rightEdgeActivation),
                                )
                            )
                        )
                ) {
                    if (rightEdgeActivation > 0f) {
                        EdgeArrowIndicator(
                            direction = ArrowDirection.RIGHT,
                            color = theme.edgeArrowColor.copy(alpha = 0.9f * rightEdgeActivation),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp)
                        )
                    }
                }
            }
        }

        // Hint text at bottom (configurable for i18n)
        BasicText(
            text = config.overlayHintText,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = theme.hintTextColor,
                letterSpacing = 0.5.sp,
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
        )
    }
}

/**
 * Direction for edge scroll indicator arrows.
 */
private enum class ArrowDirection {
    UP, DOWN, LEFT, RIGHT
}

/**
 * Canvas-drawn arrow indicator for edge scroll zones.
 * Uses Canvas instead of Unicode characters for consistent cross-platform rendering.
 */
@Composable
private fun EdgeArrowIndicator(
    direction: ArrowDirection,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.size(32.dp)
    ) {
        val strokeWidth = 3.dp.toPx()
        val arrowSize = size.minDimension * 0.6f
        val centerX = size.width / 2
        val centerY = size.height / 2
        val halfArrow = arrowSize / 2
        val offset = arrowSize * 0.25f  // Offset for double chevron

        // Draw double chevron based on direction
        when (direction) {
            ArrowDirection.UP -> {
                // First chevron (top)
                val path1 = Path().apply {
                    moveTo(centerX - halfArrow, centerY - offset)
                    lineTo(centerX, centerY - offset - halfArrow * 0.6f)
                    lineTo(centerX + halfArrow, centerY - offset)
                }
                drawPath(path1, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                // Second chevron (bottom)
                val path2 = Path().apply {
                    moveTo(centerX - halfArrow, centerY + offset)
                    lineTo(centerX, centerY + offset - halfArrow * 0.6f)
                    lineTo(centerX + halfArrow, centerY + offset)
                }
                drawPath(path2, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ArrowDirection.DOWN -> {
                // First chevron (top)
                val path1 = Path().apply {
                    moveTo(centerX - halfArrow, centerY - offset)
                    lineTo(centerX, centerY - offset + halfArrow * 0.6f)
                    lineTo(centerX + halfArrow, centerY - offset)
                }
                drawPath(path1, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                // Second chevron (bottom)
                val path2 = Path().apply {
                    moveTo(centerX - halfArrow, centerY + offset)
                    lineTo(centerX, centerY + offset + halfArrow * 0.6f)
                    lineTo(centerX + halfArrow, centerY + offset)
                }
                drawPath(path2, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ArrowDirection.LEFT -> {
                // First chevron (left)
                val path1 = Path().apply {
                    moveTo(centerX - offset, centerY - halfArrow)
                    lineTo(centerX - offset - halfArrow * 0.6f, centerY)
                    lineTo(centerX - offset, centerY + halfArrow)
                }
                drawPath(path1, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                // Second chevron (right)
                val path2 = Path().apply {
                    moveTo(centerX + offset, centerY - halfArrow)
                    lineTo(centerX + offset - halfArrow * 0.6f, centerY)
                    lineTo(centerX + offset, centerY + halfArrow)
                }
                drawPath(path2, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ArrowDirection.RIGHT -> {
                // First chevron (left)
                val path1 = Path().apply {
                    moveTo(centerX - offset, centerY - halfArrow)
                    lineTo(centerX - offset + halfArrow * 0.6f, centerY)
                    lineTo(centerX - offset, centerY + halfArrow)
                }
                drawPath(path1, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                // Second chevron (right)
                val path2 = Path().apply {
                    moveTo(centerX + offset, centerY - halfArrow)
                    lineTo(centerX + offset + halfArrow * 0.6f, centerY)
                    lineTo(centerX + offset, centerY + halfArrow)
                }
                drawPath(path2, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

/**
 * Check if a column is the source of the currently dragged card.
 */
private fun <T : SimpleKItem> isSourceColumn(dragState: DragState<T>, columnId: SimpleKId): Boolean {
    return when (dragState) {
        is DragState.Dragging -> dragState.sourceColumnId == columnId
        is DragState.OverDropTarget -> dragState.sourceColumnId == columnId
        else -> false
    }
}

/**
 * Mini representation of a column during drag.
 * Features animated border glow on hover - no scaling to keep layout stable.
 *
 * @param isDisabled When true (column at WIP limit and enforcement enabled), shows muted styling
 *                   and doesn't respond to hover
 */
@Composable
private fun <T : SimpleKItem> MiniColumnCard(
    column: SimpleKColumn<T>,
    isHovered: Boolean,
    isSourceColumn: Boolean,
    isDisabled: Boolean,
    theme: OverlayTheme,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    miniWidth: Dp,
    miniHeight: Dp,
) {
    // Column accent color (use column's color or fall back to theme's glow color)
    val columnColor = column.color ?: theme.glowColor
    val cardCount = column.items.size

    // Disabled columns don't respond to hover
    val effectiveHovered = isHovered && !isSourceColumn && !isDisabled

    // Animated glow intensity for border
    val glowAlpha by animateFloatAsState(
        targetValue = if (effectiveHovered) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "mini_column_glow"
    )

    // Animated border width
    val borderWidth by animateFloatAsState(
        targetValue = if (effectiveHovered) 2.5f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "mini_column_border"
    )

    // Card background from theme - use disabled colors when at WIP limit
    val cardBackground = when {
        isDisabled -> theme.cardBackgroundDisabled
        isSourceColumn -> theme.cardBackgroundSource
        else -> theme.cardBackground
    }

    // Fixed size outer box to prevent layout shifts from glow effect
    Box(
        modifier = modifier
            .size(width = miniWidth, height = miniHeight)
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                val size = coordinates.size.toSize()
                onBoundsChanged(
                    Rect(
                        offset = position,
                        size = size,
                    )
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        // Outer glow effect - rendered with graphicsLayer to not affect layout
        if (glowAlpha > 0f) {
            Box(
                modifier = Modifier
                    .size(width = miniWidth + 20.dp, height = miniHeight + 20.dp)
                    .graphicsLayer {
                        alpha = glowAlpha * 0.5f
                        translationX = -10.dp.toPx()
                        translationY = -10.dp.toPx()
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                theme.glowColor.copy(alpha = 0.4f),
                                theme.glowColor.copy(alpha = 0.15f),
                                Color.Transparent,
                            ),
                        ),
                        shape = RoundedCornerShape(20.dp),
                    )
            )
        }

        // Mini column card with themed colors
        Column(
            modifier = Modifier
                .size(width = miniWidth, height = miniHeight)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                )
                .clip(RoundedCornerShape(12.dp))
                .background(cardBackground)
                .border(
                    width = borderWidth.dp,
                    color = if (isDisabled) {
                        theme.cardBorderDisabled
                    } else {
                        colorLerp(theme.cardBorder, theme.cardBorderHovered, glowAlpha)
                    },
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Color indicator bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        color = when {
                            isDisabled -> columnColor.copy(alpha = 0.2f)
                            isSourceColumn -> columnColor.copy(alpha = 0.3f)
                            else -> columnColor
                        },
                        shape = RoundedCornerShape(2.dp),
                    )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Column title
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = column.title,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            isDisabled -> theme.titleColorDisabled
                            isSourceColumn -> theme.titleColorSource
                            else -> theme.titleColor
                        },
                        textAlign = TextAlign.Center,
                        lineHeight = 12.sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Card count badge
            Box(
                modifier = Modifier
                    .background(
                        color = if (isDisabled) {
                            theme.countBackgroundDisabled
                        } else {
                            colorLerp(theme.countBackground, theme.countBackgroundHovered, glowAlpha)
                        },
                        shape = RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "$cardCount",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isDisabled -> theme.countColorDisabled
                            isSourceColumn -> theme.titleColorSource
                            else -> colorLerp(theme.countColor, theme.countColorHovered, glowAlpha)
                        },
                    ),
                )
            }
        }
    }
}

/**
 * Collapsed column representation showing title rotated 90 degrees.
 */
@Composable
private fun <T : SimpleKItem> CollapsedColumn(
    column: SimpleKColumn<T>,
    config: SimpleKConfig,
    columnShape: RoundedCornerShape,
    animatedWidth: Float,
    onExpand: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(animatedWidth.dp)
            .fillMaxHeight()
            .shadow(2.dp, columnShape)
            .background(config.boardTheme.columnBackground, columnShape)
            .clip(columnShape)
            .clickable(onClick = onExpand)
            .padding(vertical = config.columnPadding.calculateTopPadding()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Expand button indicator (double chevron pointing right)
            Canvas(modifier = Modifier.size(16.dp)) {
                val strokeWidth = 2.dp.toPx()
                val path = Path().apply {
                    moveTo(size.width * 0.3f, size.height * 0.2f)
                    lineTo(size.width * 0.7f, size.height * 0.5f)
                    lineTo(size.width * 0.3f, size.height * 0.8f)
                }
                drawPath(
                    path = path,
                    color = config.boardTheme.chevronColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rotated title
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        rotationZ = 90f
                    },
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = column.title,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = config.boardTheme.chevronColor,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Card count badge
            Box(
                modifier = Modifier
                    .background(
                        config.boardTheme.columnBackgroundDragTarget.copy(alpha = 0.15f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "${column.items.size}",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = config.boardTheme.chevronColor,
                    ),
                )
            }
        }
    }
}

@Composable
private fun <T : SimpleKItem> SimpleKColumnContent(
    state: SimpleKState<T>,
    column: SimpleKColumn<T>,
    columnIndex: Int,
    config: SimpleKConfig,
    dropZoneRegistry: DropZoneRegistry,
    autoScrollController: AutoScrollController,
    boardOffset: Offset,
    cardContent: @Composable SimpleKCardScope.(card: T) -> Unit,
    columnHeader: (@Composable SimpleKHeaderScope.(column: SimpleKColumn<T>) -> Unit)?,
    columnFooter: (@Composable (column: SimpleKColumn<T>) -> Unit)?,
    emptyColumnContent: (@Composable (column: SimpleKColumn<T>) -> Unit)?,
    zoomProgress: Float = 0f,
    hapticFeedback: HapticFeedback,
    onCardMoved: ((cardId: SimpleKId, fromColumnId: SimpleKId, toColumnId: SimpleKId, fromIndex: Int, toIndex: Int) -> Unit)? = null,
    onDragStart: ((item: T) -> Unit)? = null,
    onDragEnd: ((item: T, cancelled: Boolean) -> Unit)? = null,
    onCardClick: ((item: T) -> Unit)? = null,
    canMoveCard: ((cardId: SimpleKId, toColumnId: SimpleKId) -> Boolean)? = null,
) {
    val lazyListState = rememberLazyListState()
    val shouldDisableScroll by state.shouldDisableScroll

    // Store scroll state for this column
    LaunchedEffect(column.id) {
        state.columnScrollStates[column.id] = lazyListState
    }

    // Animate column width for collapse/expand
    val isCollapsed = column.isCollapsed && config.enableColumnCollapse
    val targetWidth = if (isCollapsed) config.collapsedColumnWidth else config.columnWidth
    val animatedWidth by animateFloatAsState(
        targetValue = targetWidth.value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "column_width"
    )

    // Check if this column is a drag target (only when not collapsed)
    val isDragTarget = when (val dragState = state.cardDragState) {
        is DragState.OverDropTarget -> !isCollapsed && dragState.targetColumnId == column.id
        else -> false
    }

    // Column background color when dragging over
    val columnBackgroundAlpha by animateFloatAsState(
        targetValue = if (isDragTarget) 0.08f else 0f,
        label = "column_bg_alpha",
    )

    val columnShape = RoundedCornerShape(config.columnCornerRadius)

    // Render collapsed or expanded column
    if (isCollapsed) {
        // Collapsed column view
        CollapsedColumn(
            column = column,
            config = config,
            columnShape = columnShape,
            animatedWidth = animatedWidth,
            onExpand = { state.toggleColumnCollapsed(column.id) },
        )
    } else {
        // Accessibility: Build column state description
        val columnStateDescription = buildString {
            append("${column.items.size} cards")
            if (column.maxItems != null) {
                append(", limit ${column.maxItems}")
                if (column.isAtLimit) append(" (at limit)")
            }
            if (isDragTarget) append(", drop target active")
        }

        // Expanded column view
        Column(
            modifier = Modifier
                .width(animatedWidth.dp)
            .fillMaxHeight()
            // Accessibility: Add semantics for columns
            .semantics {
                contentDescription = "Column: ${column.title}"
                stateDescription = columnStateDescription
            }
            .shadow(2.dp, columnShape)
            .background(
                config.boardTheme.columnBackground.copy(alpha = 1f - columnBackgroundAlpha * 0.5f),
                columnShape
            )
            .background(
                config.boardTheme.columnBackgroundDragTarget.copy(alpha = columnBackgroundAlpha),
                columnShape
            )
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                val size = coordinates.size.toSize()
                dropZoneRegistry.registerColumn(
                    columnId = column.id,
                    index = columnIndex,
                    bounds = Rect(
                        offset = position,
                        size = size,
                    ),
                )
            }
            .padding(config.columnPadding),
    ) {
        // Column header
        if (columnHeader != null) {
            // Create column drag handle modifier if column reordering is enabled
            val columnDragHandleModifier = if (config.enableColumnReorder) {
                Modifier.pointerInput(column.id) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPosition = down.position
                        val longPressTimeout = viewConfiguration.longPressTimeoutMillis

                        // Detect long press
                        val longPressDetected = withTimeoutOrNull(longPressTimeout) {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull { it.id == down.id }

                                if (change == null || !change.pressed) {
                                    return@withTimeoutOrNull false
                                }

                                val distance = (change.position - startPosition).getDistance()
                                if (distance > viewConfiguration.touchSlop) {
                                    return@withTimeoutOrNull false
                                }
                            }
                            @Suppress("UNREACHABLE_CODE")
                            false
                        } == null

                        if (longPressDetected) {
                            // Haptic feedback on column drag start
                            if (config.enableHapticFeedback) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            // Start column drag
                            state.startColumnDrag(column.id, columnIndex, boardOffset + startPosition)

                            try {
                                // Track drag movement
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }

                                    if (change == null || !change.pressed) {
                                        break
                                    }

                                    change.consume()
                                    val currentPosition = boardOffset + change.position

                                    // Update column drag position
                                    state.updateColumnDrag(currentPosition)

                                    // Check for column swap based on position
                                    val columns = state.board.columns
                                    val currentColIndex = columns.indexOfFirst { it.id == column.id }
                                    if (currentColIndex >= 0) {
                                        // Find target column based on horizontal position
                                        val columnBounds = dropZoneRegistry.getColumnBounds(columns.map { it.id })
                                        for ((idx, col) in columns.withIndex()) {
                                            if (idx != currentColIndex) {
                                                val bounds = columnBounds[col.id] ?: continue
                                                if (currentPosition.x >= bounds.left && currentPosition.x <= bounds.right) {
                                                    // Swap columns
                                                    if (config.enableHapticFeedback) {
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                    state.moveColumn(currentColIndex, idx)
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }
                            } finally {
                                // End column drag
                                state.endColumnDrag()
                            }
                        }
                    }
                }
            } else {
                Modifier
            }

            val headerScope = SimpleKHeaderScopeImpl(
                itemCount = column.items.size,
                isAtLimit = column.isAtLimit,
                maxItems = column.maxItems,
                isCollapsed = column.isCollapsed,
                dragHandleModifier = columnDragHandleModifier,
                onToggleCollapse = { state.toggleColumnCollapsed(column.id) },
            )
            headerScope.columnHeader(column)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Cards
        if (column.items.isEmpty() && emptyColumnContent != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                emptyColumnContent(column)
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(config.cardSpacing),
                contentPadding = PaddingValues(vertical = 4.dp),
                // Disable scrolling when pointer is down on card or dragging
                userScrollEnabled = !shouldDisableScroll,
            ) {
                itemsIndexed(
                    items = column.items,
                    key = { _, item -> item.id.value }
                ) { index, item ->
                    DraggableCard(
                        state = state,
                        item = item,
                        columnId = column.id,
                        columnTitle = column.title,
                        index = index,
                        config = config,
                        dropZoneRegistry = dropZoneRegistry,
                        autoScrollController = autoScrollController,
                        boardOffset = boardOffset,
                        cardContent = cardContent,
                        zoomProgress = zoomProgress,
                        hapticFeedback = hapticFeedback,
                        onCardMoved = onCardMoved,
                        onDragStart = onDragStart,
                        onDragEnd = onDragEnd,
                        onCardClick = onCardClick,
                        canMoveCard = canMoveCard,
                        modifier = Modifier.animateItem(),
                    )
                }

                // Drop zone at the end of the column
                item(key = "drop_zone_end_${column.id.value}") {
                    DropZonePlaceholder(
                        state = state,
                        columnId = column.id,
                        index = column.items.size,
                    )
                }
            }
        }

        // Column footer
        if (columnFooter != null) {
            Spacer(modifier = Modifier.height(8.dp))
            columnFooter(column)
        }
    }
    }

    // Cleanup when column is removed
    DisposableEffect(column.id) {
        onDispose {
            dropZoneRegistry.unregisterColumn(column.id)
            state.columnScrollStates.remove(column.id)
        }
    }
}

@Composable
private fun <T : SimpleKItem> DraggableCard(
    state: SimpleKState<T>,
    item: T,
    columnId: SimpleKId,
    columnTitle: String,
    index: Int,
    config: SimpleKConfig,
    dropZoneRegistry: DropZoneRegistry,
    autoScrollController: AutoScrollController,
    boardOffset: Offset,
    cardContent: @Composable SimpleKCardScope.(card: T) -> Unit,
    zoomProgress: Float = 0f,
    hapticFeedback: HapticFeedback,
    onCardMoved: ((cardId: SimpleKId, fromColumnId: SimpleKId, toColumnId: SimpleKId, fromIndex: Int, toIndex: Int) -> Unit)? = null,
    onDragStart: ((item: T) -> Unit)? = null,
    onDragEnd: ((item: T, cancelled: Boolean) -> Unit)? = null,
    onCardClick: ((item: T) -> Unit)? = null,
    canMoveCard: ((cardId: SimpleKId, toColumnId: SimpleKId) -> Boolean)? = null,
    modifier: Modifier = Modifier,
) {
    var cardPosition by remember { mutableStateOf(Offset.Zero) }

    // Track if this card is being dragged
    val isDragging = when (val dragState = state.cardDragState) {
        is DragState.Dragging -> dragState.item.id == item.id
        is DragState.OverDropTarget -> dragState.item.id == item.id
        else -> false
    }

    // Animate alpha when being dragged (ghost card left behind)
    val dragAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.3f else 1f,
        label = "card_alpha",
    )

    // Calculate zoom-based scale and alpha for non-dragged cards
    // Cards scale to 0.3 and fade to 0 as zoom progresses
    val zoomScale = lerp(1f, 0.3f, zoomProgress)
    val zoomAlpha = lerp(1f, 0f, zoomProgress)

    // Accessibility: Compute state description based on drag state
    val cardStateDescription = when {
        isDragging -> "Being dragged"
        else -> "In column $columnTitle, position ${index + 1}"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            // Accessibility: Add semantics for screen readers
            .semantics {
                contentDescription = "Kanban card, item ${index + 1} of column $columnTitle"
                stateDescription = cardStateDescription
                role = Role.Button
            }
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                val size = coordinates.size.toSize()
                cardPosition = position
                dropZoneRegistry.registerCard(
                    cardId = item.id,
                    columnId = columnId,
                    index = index,
                    bounds = Rect(
                        offset = position,
                        size = size,
                    ),
                )
                // Store card height for placeholder sizing
                state.cardHeights[item.id] = size.height
            }
            .alpha(dragAlpha)
            .then(
                // Only apply zoom animation to non-dragged cards
                if (!isDragging && zoomProgress > 0f) {
                    Modifier.graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = zoomScale
                        scaleY = zoomScale
                        alpha = zoomAlpha
                    }
                } else {
                    Modifier
                }
            )
            .pointerInput(item.id, config.enableCardDrag) {
                if (!config.enableCardDrag) return@pointerInput

                awaitEachGesture {
                    // Wait for initial touch - don't consume yet to allow scroll
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPosition = down.position
                    val longPressTimeout = viewConfiguration.longPressTimeoutMillis

                    // Try to detect long press without blocking scroll
                    // Only consume events once long press is confirmed
                    // Returns: null = long press, false = finger lifted (click), true = moved too far (scroll)
                    val gestureResult = withTimeoutOrNull(longPressTimeout) {
                        while (true) {
                            // Use Main pass to observe without blocking
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id }

                            if (change == null || !change.pressed) {
                                // Finger lifted - this is a click (not a long press)
                                return@withTimeoutOrNull false
                            }

                            // Check if moved too far - this is a scroll gesture
                            val distance = (change.position - startPosition).getDistance()
                            if (distance > viewConfiguration.touchSlop) {
                                // User is scrolling, don't intercept
                                return@withTimeoutOrNull true
                            }
                        }
                        @Suppress("UNREACHABLE_CODE")
                        false
                    }

                    // gestureResult: null = long press, false = click, true = scroll
                    val longPressDetected = gestureResult == null
                    val wasClicked = gestureResult == false

                    // Handle click event (finger lifted without moving, before long press timeout)
                    if (wasClicked) {
                        onCardClick?.invoke(item)
                    }

                    if (longPressDetected) {
                        // NOW we take over - disable scroll and consume events
                        state.setPointerDownOnCard(true)

                        // Haptic feedback on drag start
                        if (config.enableHapticFeedback) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }

                        try {
                            // Long press detected - start drag
                            val dragStartPosition = cardPosition + startPosition
                            // startPosition is where the user touched relative to the card's top-left
                            state.startCardDrag(item, columnId, index, dragStartPosition, startPosition)

                            // Invoke onDragStart callback
                            onDragStart?.invoke(item)

                            // Capture source column bounds for exit detection
                            // Zoom-out only triggers when dragging OUT of the column
                            val sourceColumnBounds = dropZoneRegistry.getColumnBounds(columnId)

                            // Track current position in the list (updates when we move)
                            var currentColumnId = columnId
                            var currentIndex = index

                            // Debounce: minimum time between moves (prevents rapid oscillation)
                            val timeSource = TimeSource.Monotonic
                            var lastMoveTime = timeSource.markNow()
                            val moveDebounceMs = 150L

                            // Track drag movement
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id }

                                if (change == null || !change.pressed) {
                                    break
                                }

                                change.consume()
                                val currentPosition = cardPosition + change.position

                                // Check if we've left the source column (trigger zoom)
                                if (config.enableZoomOutDrag &&
                                    state.zoomPhase == ZoomPhase.Normal &&
                                    sourceColumnBounds != null
                                ) {
                                    val hasLeftColumn = currentPosition.x < sourceColumnBounds.left ||
                                        currentPosition.x > sourceColumnBounds.right
                                    if (hasLeftColumn) {
                                        state.startZoomOutTransition(config.zoomOutDurationMillis)
                                    }
                                }

                                // Only do immediate moves when NOT zoomed
                                // During zoom, the overlay handles drop target detection
                                if (state.zoomPhase == ZoomPhase.Normal) {
                                    // Find drop target using card center
                                    val cardHeight = state.cardHeights[item.id] ?: 0f
                                    val cardTopY = currentPosition.y - startPosition.y
                                    val cardCenterY = cardTopY + cardHeight / 2f
                                    val cardCenterPosition = Offset(
                                        x = currentPosition.x,
                                        y = cardCenterY,
                                    )
                                    val dropTarget = dropZoneRegistry.findDropTarget(
                                        position = cardCenterPosition,
                                        draggedCardId = item.id,
                                        draggedFromColumnId = currentColumnId,
                                    )

                                    // Check if we should move the card immediately
                                    val canMove = lastMoveTime.elapsedNow().inWholeMilliseconds >= moveDebounceMs

                                    if (canMove && dropTarget != null) {
                                        val targetColumnId = dropTarget.columnId
                                        val targetIndex = dropTarget.index

                                        // Only move if position actually changed
                                        val positionChanged = targetColumnId != currentColumnId ||
                                            targetIndex != currentIndex

                                        // Check if move is allowed by canMoveCard callback
                                        val moveAllowed = canMoveCard?.invoke(item.id, targetColumnId) != false

                                        if (positionChanged && moveAllowed) {
                                            // Haptic feedback on card position change
                                            if (config.enableHapticFeedback) {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }

                                            // Store source info for callback
                                            val fromColumnId = currentColumnId
                                            val fromIndex = currentIndex

                                            // Move card immediately - LazyColumn animateItem handles animation
                                            state.moveCard(item.id, targetColumnId, targetIndex)
                                            lastMoveTime = timeSource.markNow()

                                            // Invoke onCardMoved callback
                                            onCardMoved?.invoke(item.id, fromColumnId, targetColumnId, fromIndex, targetIndex)

                                            // Update our tracking
                                            currentColumnId = targetColumnId
                                            currentIndex = targetIndex
                                        }
                                    }

                                    // Update drag visual position
                                    state.updateCardDrag(
                                        offset = currentPosition,
                                        targetColumnId = dropTarget?.columnId,
                                        targetIndex = dropTarget?.index,
                                    )
                                } else {
                                    // Zoomed mode - just update visual position
                                    // Overlay handles drop target via hoveredOverlayColumnId
                                    state.updateCardDrag(
                                        offset = currentPosition,
                                        targetColumnId = null,
                                        targetIndex = null,
                                    )
                                }
                            }

                            // Drag ended - card is already in final position
                            autoScrollController.stopAll()
                            state.endCardDrag(canMoveCard)

                            // Invoke onDragEnd callback (cancelled = false since drag completed)
                            onDragEnd?.invoke(item, false)

                            // Zoom back in if we started zooming (ZoomingOut or Miniature phase)
                            // Don't trigger if already ZoomingIn
                            val phase = state.zoomPhase
                            if (config.enableZoomOutDrag &&
                                (phase == ZoomPhase.ZoomingOut || phase == ZoomPhase.Miniature)) {
                                state.startZoomInTransition(config.zoomInDurationMillis)
                            }
                        } finally {
                            state.setPointerDownOnCard(false)
                            // Safety: ensure zoom resets if drag ends unexpectedly
                            val finalPhase = state.zoomPhase
                            if (config.enableZoomOutDrag &&
                                (finalPhase == ZoomPhase.ZoomingOut || finalPhase == ZoomPhase.Miniature)) {
                                state.startZoomInTransition(config.zoomInDurationMillis)
                            }
                        }
                    }
                    // If not long press, gesture ends and scroll can handle it
                }
            },
    ) {
        // Check if this card can move to next column
        val columns = state.board.columns
        val currentColumnIndex = columns.indexOfFirst { it.id == columnId }
        val hasNextColumn = currentColumnIndex >= 0 && currentColumnIndex < columns.size - 1
        val nextColumn = if (hasNextColumn) columns[currentColumnIndex + 1] else null
        val nextColumnId = nextColumn?.id

        // Hide chevron if next column is at WIP limit and enforcement is enabled
        val nextColumnAtLimit = config.enforceWipLimits && nextColumn?.isAtLimit == true

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Card content takes available space
            Box(modifier = Modifier.weight(1f)) {
                val cardScope = SimpleKCardScopeImpl(
                    isDragging = isDragging,
                    isDropTarget = false,
                    dragProgress = if (isDragging) 1f else 0f,
                    cardIndex = index,
                    columnId = columnId,
                    dragHandleModifier = Modifier,
                )
                cardScope.cardContent(item)
            }

            // Chevron button to move to next column (only if enabled, not last column, and next column not at limit)
            if (config.showChevronButtons && hasNextColumn && nextColumn != null && nextColumnId != null && !isDragging && !nextColumnAtLimit) {
                val nextColumnTitle = nextColumn.title
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(32.dp)
                        // Accessibility: Add semantics for the move action button
                        .semantics {
                            contentDescription = "Move card to $nextColumnTitle"
                            role = Role.Button
                        }
                        .clickable {
                            // Haptic feedback on chevron tap
                            if (config.enableHapticFeedback) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            // Move card to end of next column
                            state.moveCard(item.id, nextColumnId, nextColumn.items.size)
                        }
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Chevron icon using canvas
                    Canvas(
                        modifier = Modifier.size(16.dp)
                    ) {
                        val strokeWidth = 2.dp.toPx()
                        val chevronColor = config.boardTheme.chevronColor

                        // Draw chevron pointing right: >
                        val path = Path().apply {
                            moveTo(size.width * 0.3f, size.height * 0.2f)
                            lineTo(size.width * 0.7f, size.height * 0.5f)
                            lineTo(size.width * 0.3f, size.height * 0.8f)
                        }
                        drawPath(
                            path = path,
                            color = chevronColor,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            )
                        )
                    }
                }
            }
        }
    }

    // Cleanup when card is removed
    DisposableEffect(item.id, columnId) {
        onDispose {
            dropZoneRegistry.unregisterCard(item.id, columnId)
            state.cardHeights.remove(item.id)
        }
    }
}

@Composable
private fun <T : SimpleKItem> DropZonePlaceholder(
    state: SimpleKState<T>,
    columnId: SimpleKId,
    index: Int,
) {
    // Placeholder for drop zone at end of column
    // No visible indicator needed - cards shift to show drop position
}

@Composable
private fun <T : SimpleKItem> DragOverlay(
    state: SimpleKState<T>,
    config: SimpleKConfig,
    boardOffset: Offset,
    cardContent: @Composable SimpleKCardScope.(card: T) -> Unit,
) {
    val dragState = state.cardDragState

    val item: T? = when (dragState) {
        is DragState.Dragging -> dragState.item
        is DragState.OverDropTarget -> dragState.item
        else -> null
    }

    val dragOffset: Offset? = when (dragState) {
        is DragState.Dragging -> dragState.currentOffset
        is DragState.OverDropTarget -> dragState.currentOffset
        else -> null
    }

    // Touch offset within the card - where the user initially touched
    val touchOffsetInCard: Offset = when (dragState) {
        is DragState.Dragging -> dragState.touchOffsetInCard
        is DragState.OverDropTarget -> dragState.touchOffsetInCard
        else -> Offset.Zero
    }

    if (item != null && dragOffset != null) {
        val elevation = remember { Animatable(0f) }
        val scale = remember { Animatable(1f) }

        LaunchedEffect(item.id) {
            launch { elevation.animateTo(12f) }
            launch { scale.animateTo(config.dragOverlayScale) }
        }

        // Cursor indicator - shows exact finger position
        // Visible during zoom-out to help target miniature columns
        val zoomProgress = state.zoomProgress
        val cursorAlpha = zoomProgress // Only visible during zoom
        val cursorSize = 16.dp

        if (cursorAlpha > 0f) {
            Box(
                modifier = Modifier
                    .offset {
                        // Center the indicator at the drag position
                        // Adjust from root coordinates to local coordinates
                        val halfSizePx = (cursorSize.toPx() / 2).roundToInt()
                        IntOffset(
                            (dragOffset.x - boardOffset.x).roundToInt() - halfSizePx,
                            (dragOffset.y - boardOffset.y).roundToInt() - halfSizePx
                        )
                    }
                    .size(cursorSize)
                    .graphicsLayer { alpha = cursorAlpha }
                    .background(config.boardTheme.accentColor, RoundedCornerShape(50))
                    .border(2.dp, config.boardTheme.cardBackground, RoundedCornerShape(50))
            )
        }

        // Dragged card
        val cardShape = RoundedCornerShape(config.cardCornerRadius)
        Box(
            modifier = Modifier
                .offset {
                    // Position card so the touch point stays under the user's finger
                    // Adjust from root coordinates to local coordinates
                    IntOffset(
                        (dragOffset.x - boardOffset.x - touchOffsetInCard.x).roundToInt(),
                        (dragOffset.y - boardOffset.y - touchOffsetInCard.y).roundToInt()
                    )
                }
                .width(config.columnWidth - 24.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    shadowElevation = elevation.value
                    // Fade out the card during zoom so cursor indicator is more visible
                    alpha = lerp(1f, 0.7f, zoomProgress)
                }
                .shadow(elevation.value.dp, cardShape)
                .background(config.boardTheme.cardBackground, cardShape),
        ) {
            // Get source column and index from drag state
            val sourceColumnId = when (dragState) {
                is DragState.Dragging -> dragState.sourceColumnId
                is DragState.OverDropTarget -> dragState.sourceColumnId
                else -> SimpleKId("")
            }
            val sourceIndex = when (dragState) {
                is DragState.Dragging -> dragState.sourceIndex
                is DragState.OverDropTarget -> dragState.sourceIndex
                else -> 0
            }

            val cardScope = SimpleKCardScopeImpl(
                isDragging = true,
                isDropTarget = false,
                dragProgress = 1f,
                cardIndex = sourceIndex,
                columnId = sourceColumnId,
                dragHandleModifier = Modifier,
            )
            cardScope.cardContent(item)
        }
    }
}
