package io.github.kankan.demo.components.cards

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kankan.demo.theme.KankanTheme
import io.github.simplek.scope.SimpleKCardScope

enum class TrendDirection {
    UP,
    DOWN,
    NEUTRAL,
}

/**
 * A metric/stats card with large value display and trend indicator.
 * Great for dashboard-style Kanban boards tracking KPIs.
 */
@Composable
fun SimpleKCardScope.MetricCard(
    label: String,
    value: String,
    trend: TrendDirection = TrendDirection.NEUTRAL,
    trendValue: String? = null,
    sparklineData: List<Float>? = null,
    accentColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val colors = KankanTheme.colors
    val accent = accentColor ?: colors.accent

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 2.dp,
        animationSpec = spring(),
        label = "elevation",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .padding(16.dp),
    ) {
        // Label
        BasicText(
            text = label.uppercase(),
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textTertiary,
                letterSpacing = 1.sp,
            ),
        )

        Spacer(Modifier.height(8.dp))

        // Value and Trend Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            // Large value
            BasicText(
                text = value,
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    letterSpacing = (-1).sp,
                ),
            )

            // Trend indicator
            if (trend != TrendDirection.NEUTRAL || trendValue != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Trend arrow
                    val trendColor = when (trend) {
                        TrendDirection.UP -> colors.success
                        TrendDirection.DOWN -> colors.error
                        TrendDirection.NEUTRAL -> colors.textTertiary
                    }

                    Canvas(modifier = Modifier.size(12.dp)) {
                        val path = Path().apply {
                            when (trend) {
                                TrendDirection.UP -> {
                                    moveTo(size.width / 2, 0f)
                                    lineTo(size.width, size.height)
                                    lineTo(0f, size.height)
                                    close()
                                }
                                TrendDirection.DOWN -> {
                                    moveTo(0f, 0f)
                                    lineTo(size.width, 0f)
                                    lineTo(size.width / 2, size.height)
                                    close()
                                }
                                TrendDirection.NEUTRAL -> {
                                    moveTo(0f, size.height / 2)
                                    lineTo(size.width, size.height / 2)
                                }
                            }
                        }

                        if (trend == TrendDirection.NEUTRAL) {
                            drawLine(
                                color = trendColor,
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width, size.height / 2),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                        } else {
                            drawPath(path, trendColor)
                        }
                    }

                    if (trendValue != null) {
                        BasicText(
                            text = trendValue,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = trendColor,
                            ),
                        )
                    }
                }
            }
        }

        // Sparkline
        if (sparklineData != null && sparklineData.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
            ) {
                val maxValue = sparklineData.maxOrNull() ?: 1f
                val minValue = sparklineData.minOrNull() ?: 0f
                val range = (maxValue - minValue).coerceAtLeast(0.01f)

                val path = Path()
                val stepX = size.width / (sparklineData.size - 1).coerceAtLeast(1)

                sparklineData.forEachIndexed { index, value ->
                    val normalizedY = (value - minValue) / range
                    val x = index * stepX
                    val y = size.height - (normalizedY * size.height)

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = accent.copy(alpha = 0.8f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                // Dots at data points
                sparklineData.forEachIndexed { index, value ->
                    val normalizedY = (value - minValue) / range
                    val x = index * stepX
                    val y = size.height - (normalizedY * size.height)

                    drawCircle(
                        color = accent,
                        radius = 3.dp.toPx(),
                        center = Offset(x, y),
                    )
                }
            }
        }

        // Accent bar at bottom
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(accent.copy(alpha = 0.3f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(3.dp)
                    .background(accent, RoundedCornerShape(1.5.dp)),
            )
        }
    }
}
