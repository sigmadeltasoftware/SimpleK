package io.github.kankan.demo.components.cards

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kankan.demo.theme.KankanTheme
import io.github.simplek.model.CardLabel
import io.github.simplek.scope.SimpleKCardScope

/**
 * A rich media card with a gradient header.
 * Perfect for content boards, media management, or visual-heavy workflows.
 */
@Composable
fun SimpleKCardScope.GradientCard(
    title: String,
    subtitle: String? = null,
    gradientColors: List<Color>,
    labels: List<CardLabel> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val colors = KankanTheme.colors

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
            .background(colors.card),
    ) {
        // Gradient header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    brush = Brush.linearGradient(colors = gradientColors)
                ),
            contentAlignment = Alignment.BottomStart,
        ) {
            // Optional: subtle pattern overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.15f),
                            )
                        )
                    ),
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            // Labels row
            if (labels.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    labels.take(3).forEach { label ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(label.color, CircleShape),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    if (labels.size > 3) {
                        BasicText(
                            text = "+${labels.size - 3}",
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = colors.textTertiary,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Title
            BasicText(
                text = title,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    lineHeight = 18.sp,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Subtitle
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = subtitle,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = colors.textTertiary,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Predefined gradient presets for GradientCard
 */
object GradientPresets {
    val Sunset = listOf(Color(0xFFFF6B6B), Color(0xFFFFA726))
    val Ocean = listOf(Color(0xFF4FC3F7), Color(0xFF29B6F6), Color(0xFF0288D1))
    val Forest = listOf(Color(0xFF66BB6A), Color(0xFF43A047))
    val Lavender = listOf(Color(0xFFCE93D8), Color(0xFFAB47BC))
    val Midnight = listOf(Color(0xFF5C6BC0), Color(0xFF3949AB))
    val Coral = listOf(Color(0xFFFF8A80), Color(0xFFFF5252))
    val Teal = listOf(Color(0xFF26A69A), Color(0xFF00897B))
    val Gold = listOf(Color(0xFFFFD54F), Color(0xFFFFB300))
}
