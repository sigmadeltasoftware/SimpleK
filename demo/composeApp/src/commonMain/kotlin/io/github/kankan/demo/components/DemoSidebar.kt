package io.github.kankan.demo.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.simplek.config.BoardTheme
import io.github.simplek.config.SimpleKConfig
import io.github.simplek.config.OverlayTheme
import io.github.kankan.demo.theme.KankanTheme
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import kankan.demo.composeapp.generated.resources.Res
import kankan.demo.composeapp.generated.resources.simplek

enum class CardStyle {
    DEFAULT,
    COMPACT,
    RICH,
}

@Composable
fun DemoSidebar(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    cardStyle: CardStyle,
    onCardStyleChange: (CardStyle) -> Unit,
    config: SimpleKConfig,
    onConfigChange: (SimpleKConfig) -> Unit,
    boardStats: BoardStats,
    onAddCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KankanTheme.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(colors.surface)
            .border(
                width = 1.dp,
                color = colors.border,
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
            )
            .padding(20.dp)
            .verticalScroll(scrollState),
    ) {
        // Header Logo
        Image(
            painter = painterResource(Res.drawable.simplek),
            contentDescription = "SimpleK Logo",
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentScale = ContentScale.Fit,
        )

        Spacer(Modifier.height(4.dp))

        BasicText(
            text = "DEMO",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.accent,
                letterSpacing = 3.sp,
            ),
        )

        Spacer(Modifier.height(32.dp))

        // Theme Toggle
        SectionHeader("Appearance")
        Spacer(Modifier.height(12.dp))

        ThemeToggle(
            isDark = isDarkTheme,
            onToggle = onThemeToggle,
        )

        Spacer(Modifier.height(28.dp))

        // Card Style
        SectionHeader("Card Style")
        Spacer(Modifier.height(12.dp))

        CardStylePicker(
            selected = cardStyle,
            onSelect = onCardStyleChange,
        )

        Spacer(Modifier.height(28.dp))

        // Config Presets
        SectionHeader("Layout")
        Spacer(Modifier.height(12.dp))

        ConfigPresets(
            currentConfig = config,
            onConfigChange = onConfigChange,
            isDark = isDarkTheme,
        )

        Spacer(Modifier.height(28.dp))

        // Feature Toggles
        SectionHeader("Features")
        Spacer(Modifier.height(12.dp))

        FeatureToggle(
            label = "Zoom-out drag",
            enabled = config.enableZoomOutDrag,
            onToggle = { onConfigChange(config.copy(enableZoomOutDrag = it)) },
        )

        Spacer(Modifier.height(8.dp))

        FeatureToggle(
            label = "Column reorder",
            enabled = config.enableColumnReorder,
            onToggle = { onConfigChange(config.copy(enableColumnReorder = it)) },
        )

        Spacer(Modifier.height(28.dp))

        // Stats
        SectionHeader("Board Stats")
        Spacer(Modifier.height(12.dp))

        StatsDisplay(stats = boardStats)

        Spacer(Modifier.height(28.dp))

        // Add Card Button
        AddCardActionButton(onClick = onAddCard)

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    val colors = KankanTheme.colors
    BasicText(
        text = text.uppercase(),
        style = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textTertiary,
            letterSpacing = 1.5.sp,
        ),
    )
}

@Composable
private fun ThemeToggle(
    isDark: Boolean,
    onToggle: () -> Unit,
) {
    val colors = KankanTheme.colors

    val rotation by animateFloatAsState(
        targetValue = if (isDark) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "theme_rotation",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant)
            .clickable(onClick = onToggle)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = if (isDark) "Dark Mode" else "Light Mode",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
            ),
        )

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.15f))
                .rotate(rotation),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                if (isDark) {
                    // Moon icon
                    val moonPath = Path().apply {
                        val cx = size.width / 2
                        val cy = size.height / 2
                        val r = size.width * 0.4f

                        moveTo(cx + r * 0.2f, cy - r)
                        for (i in 0..360 step 10) {
                            val angle = kotlin.math.PI / 180.0 *(i.toDouble())
                            val x = cx + r * kotlin.math.cos(angle).toFloat()
                            val y = cy + r * kotlin.math.sin(angle).toFloat()
                            lineTo(x, y)
                        }
                    }
                    drawCircle(
                        color = Color(0xFF22D3EE),
                        radius = size.width * 0.35f,
                    )
                    drawCircle(
                        color = Color(0xFF1A1A1A),
                        radius = size.width * 0.25f,
                        center = Offset(size.width * 0.65f, size.height * 0.35f),
                    )
                } else {
                    // Sun icon
                    drawCircle(
                        color = Color(0xFFE85D04),
                        radius = size.width * 0.25f,
                    )
                    // Rays
                    val rayLength = size.width * 0.15f
                    val rayStart = size.width * 0.35f
                    for (i in 0 until 8) {
                        val angle = kotlin.math.PI / 180.0 *((i * 45).toDouble())
                        val startX = size.width / 2 + rayStart * kotlin.math.cos(angle).toFloat()
                        val startY = size.height / 2 + rayStart * kotlin.math.sin(angle).toFloat()
                        val endX = size.width / 2 + (rayStart + rayLength) * kotlin.math.cos(angle).toFloat()
                        val endY = size.height / 2 + (rayStart + rayLength) * kotlin.math.sin(angle).toFloat()
                        drawLine(
                            color = Color(0xFFE85D04),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardStylePicker(
    selected: CardStyle,
    onSelect: (CardStyle) -> Unit,
) {
    val colors = KankanTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CardStyle.entries.forEach { style ->
            val isSelected = style == selected

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) colors.accent.copy(alpha = 0.12f) else Color.Transparent
                    )
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) colors.accent else colors.border,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable { onSelect(style) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Radio indicator
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .border(
                            width = 2.dp,
                            color = if (isSelected) colors.accent else colors.textTertiary,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(colors.accent, CircleShape),
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    BasicText(
                        text = when (style) {
                            CardStyle.DEFAULT -> "Default"
                            CardStyle.COMPACT -> "Compact"
                            CardStyle.RICH -> "Rich Media"
                        },
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) colors.accent else colors.textPrimary,
                        ),
                    )

                    BasicText(
                        text = when (style) {
                            CardStyle.DEFAULT -> "Full featured cards"
                            CardStyle.COMPACT -> "Dense, minimal cards"
                            CardStyle.RICH -> "Cards with gradients"
                        },
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = colors.textTertiary,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigPresets(
    currentConfig: SimpleKConfig,
    onConfigChange: (SimpleKConfig) -> Unit,
    isDark: Boolean,
) {
    val colors = KankanTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            "Default" to SimpleKConfig.Default,
            "Compact" to SimpleKConfig.Compact,
            "Wide" to SimpleKConfig.Wide,
        ).forEach { (name, config) ->
            val isSelected = currentConfig.columnWidth == config.columnWidth

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) colors.accent else colors.surfaceVariant
                    )
                    .clickable {
                        // Preserve feature toggles when changing preset
                        onConfigChange(
                            config.copy(
                                enableZoomOutDrag = currentConfig.enableZoomOutDrag,
                                enableColumnReorder = currentConfig.enableColumnReorder,
                                overlayTheme = if (isDark) OverlayTheme.Dark else OverlayTheme.Light,
                                boardTheme = if (isDark) BoardTheme.Dark else BoardTheme.Light,
                            )
                        )
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = name,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) Color.White else colors.textSecondary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun FeatureToggle(
    label: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val colors = KankanTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle(!enabled) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                fontSize = 14.sp,
                color = colors.textPrimary,
            ),
        )

        // Toggle switch
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (enabled) colors.accent else colors.border)
                .padding(2.dp),
        ) {
            val offsetX by animateFloatAsState(
                targetValue = if (enabled) 1f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessHigh),
                label = "toggle",
            )

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .padding(start = (offsetX * 20).dp)
                    .background(Color.White, CircleShape),
            )
        }
    }
}

data class BoardStats(
    val totalCards: Int,
    val columnsCount: Int,
    val inProgressCount: Int,
    val completedCount: Int,
)

@Composable
private fun StatsDisplay(stats: BoardStats) {
    val colors = KankanTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatItem("Cards", stats.totalCards.toString(), colors)
            StatItem("Columns", stats.columnsCount.toString(), colors)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatItem("In Progress", stats.inProgressCount.toString(), colors)
            StatItem("Done", stats.completedCount.toString(), colors)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, colors: io.github.kankan.demo.theme.KankanColors) {
    Column {
        BasicText(
            text = value,
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            ),
        )
        BasicText(
            text = label,
            style = TextStyle(
                fontSize = 12.sp,
                color = colors.textTertiary,
            ),
        )
    }
}

@Composable
private fun AddCardActionButton(onClick: () -> Unit) {
    val colors = KankanTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.accent)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "+",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                ),
            )
            BasicText(
                text = "Add Card",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                ),
            )
        }
    }
}
