package io.github.kankan.demo.feature.board.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.simplek.SimpleKBoard
import io.github.simplek.components.DefaultColumnHeader
import io.github.kankan.demo.components.AddCardSheet
import io.github.kankan.demo.components.CardStyle
import io.github.kankan.demo.components.DemoSidebar
import io.github.kankan.demo.components.cards.GradientCard
import io.github.kankan.demo.components.cards.GradientPresets
import io.github.kankan.demo.components.cards.ThemedCompactCard
import io.github.kankan.demo.components.cards.ThemedTaskCard
import io.github.kankan.demo.feature.board.BoardContract
import io.github.kankan.demo.feature.board.BoardViewModel
import io.github.kankan.demo.theme.KankanTheme
import io.github.simplek.model.DefaultCard
import io.github.simplek.state.rememberSimpleKState
import kotlinx.coroutines.flow.collectLatest

@Composable
fun BoardScreen(
    viewModel: BoardViewModel,
    onShowSnackbar: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val colors = KankanTheme.colors

    // Collect side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffects.collectLatest { sideEffect ->
            when (sideEffect) {
                is BoardContract.SideEffect.ShowMessage -> {
                    onShowSnackbar(sideEffect.message)
                }
            }
        }
    }

    // Get safe area insets
    val safeAreaInsets = WindowInsets.safeDrawing.asPaddingValues()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.accent,
                )
            }

            state.board != null -> {
                val board = state.board!!
                val kanbanState = rememberSimpleKState(board)

                Row(modifier = Modifier.fillMaxSize()) {
                    // Animated Sidebar
                    AnimatedVisibility(
                        visible = state.isSidebarVisible,
                        enter = slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        ) + fadeIn(),
                        exit = slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        ) + fadeOut(),
                    ) {
                        DemoSidebar(
                            isDarkTheme = state.isDarkTheme,
                            onThemeToggle = { viewModel.toggleTheme() },
                            cardStyle = state.cardStyle,
                            onCardStyleChange = { viewModel.setCardStyle(it) },
                            config = state.config,
                            onConfigChange = { viewModel.setConfig(it) },
                            boardStats = viewModel.getBoardStats(),
                            onAddCard = { viewModel.showAddCardSheet() },
                            modifier = Modifier
                                .padding(
                                    top = safeAreaInsets.calculateTopPadding(),
                                    bottom = safeAreaInsets.calculateBottomPadding(),
                                ),
                        )
                    }

                    // Kanban Board
                    SimpleKBoard(
                        state = kanbanState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(
                                top = safeAreaInsets.calculateTopPadding(),
                                bottom = safeAreaInsets.calculateBottomPadding(),
                                end = safeAreaInsets.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                            ),
                        config = state.config,
                        cardContent = { card: DefaultCard ->
                            when (state.cardStyle) {
                                CardStyle.DEFAULT -> {
                                    ThemedTaskCard(card = card)
                                }
                                CardStyle.COMPACT -> {
                                    ThemedCompactCard(
                                        title = card.title,
                                        labels = card.labels,
                                        priority = card.priority,
                                    )
                                }
                                CardStyle.RICH -> {
                                    val gradient = when (card.priority) {
                                        io.github.simplek.model.CardPriority.HIGH,
                                        io.github.simplek.model.CardPriority.URGENT -> GradientPresets.Coral
                                        io.github.simplek.model.CardPriority.MEDIUM -> GradientPresets.Gold
                                        io.github.simplek.model.CardPriority.LOW -> GradientPresets.Forest
                                        else -> {
                                            val gradients = listOf(
                                                GradientPresets.Ocean,
                                                GradientPresets.Lavender,
                                                GradientPresets.Teal,
                                                GradientPresets.Midnight,
                                            )
                                            gradients[card.title.length % gradients.size]
                                        }
                                    }
                                    GradientCard(
                                        title = card.title,
                                        subtitle = card.description,
                                        gradientColors = gradient,
                                        labels = card.labels,
                                    )
                                }
                            }
                        },
                        columnHeader = { column ->
                            DefaultColumnHeader(
                                title = column.title,
                                accentColor = column.color,
                            )
                        },
                        columnFooter = { column ->
                            AddCardButton(
                                onClick = {
                                    viewModel.addCard(column.id, "New Task")
                                },
                            )
                        },
                        emptyColumnContent = { _ ->
                            EmptyColumnPlaceholder()
                        },
                    )
                }

                // FAB to toggle sidebar
                SidebarToggleFab(
                    isExpanded = state.isSidebarVisible,
                    onClick = { viewModel.toggleSidebar() },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = 20.dp,
                            bottom = safeAreaInsets.calculateBottomPadding() + 20.dp,
                        ),
                )

                // Add Card Sheet
                AddCardSheet(
                    isVisible = state.showAddCardSheet,
                    columns = board.columns,
                    onDismiss = { viewModel.hideAddCardSheet() },
                    onAddCard = { viewModel.addCard(it) },
                )
            }
        }
    }
}

@Composable
private fun SidebarToggleFab(
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KankanTheme.colors

    // Animate rotation for the hamburger/arrow icon
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 180f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "fab_rotation",
    )

    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(colors.accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Animated hamburger/arrow icon
        Canvas(
            modifier = Modifier
                .size(24.dp)
                .rotate(rotation),
        ) {
            val lineColor = Color.White
            val strokeWidth = 2.5.dp.toPx()
            val spacing = size.height / 4

            if (isExpanded) {
                // Arrow pointing left (sidebar visible, click to collapse)
                // Top diagonal
                drawLine(
                    color = lineColor,
                    start = Offset(size.width * 0.65f, size.height * 0.25f),
                    end = Offset(size.width * 0.35f, size.height * 0.5f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                // Bottom diagonal
                drawLine(
                    color = lineColor,
                    start = Offset(size.width * 0.65f, size.height * 0.75f),
                    end = Offset(size.width * 0.35f, size.height * 0.5f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            } else {
                // Hamburger menu icon (sidebar hidden, click to show)
                // Top line
                drawLine(
                    color = lineColor,
                    start = Offset(size.width * 0.2f, spacing),
                    end = Offset(size.width * 0.8f, spacing),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                // Middle line
                drawLine(
                    color = lineColor,
                    start = Offset(size.width * 0.2f, size.height / 2),
                    end = Offset(size.width * 0.8f, size.height / 2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                // Bottom line
                drawLine(
                    color = lineColor,
                    start = Offset(size.width * 0.2f, size.height - spacing),
                    end = Offset(size.width * 0.8f, size.height - spacing),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun AddCardButton(
    onClick: () -> Unit,
) {
    val colors = KankanTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = "+",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textTertiary,
            ),
        )
        BasicText(
            text = "Add card",
            style = TextStyle(
                fontSize = 14.sp,
                color = colors.textTertiary,
            ),
        )
    }
}

@Composable
private fun EmptyColumnPlaceholder() {
    val colors = KankanTheme.colors

    BasicText(
        text = "Drop cards here",
        style = TextStyle(
            fontSize = 14.sp,
            color = colors.textTertiary,
        ),
    )
}
