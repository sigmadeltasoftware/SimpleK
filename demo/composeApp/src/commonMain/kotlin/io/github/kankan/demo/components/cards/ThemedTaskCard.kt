package io.github.kankan.demo.components.cards

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.kankan.demo.theme.KankanTheme
import io.github.kankan.demo.theme.PriorityColors
import io.github.simplek.model.CardLabel
import io.github.simplek.model.CardPriority
import io.github.simplek.model.DefaultCard
import io.github.simplek.scope.SimpleKCardScope

/**
 * A themed task card that respects the current theme colors.
 * Full-featured card with priority, labels, title, and description.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimpleKCardScope.ThemedTaskCard(
    card: DefaultCard,
    modifier: Modifier = Modifier,
) {
    val colors = KankanTheme.colors

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 2.dp,
        animationSpec = spring(),
        label = "elevation",
    )

    val priorityColor = when (card.priority) {
        CardPriority.NONE -> null
        CardPriority.LOW -> PriorityColors.Low
        CardPriority.MEDIUM -> PriorityColors.Medium
        CardPriority.HIGH -> PriorityColors.High
        CardPriority.URGENT -> PriorityColors.Urgent
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(colors.card),
    ) {
        // Image (if present)
        if (card.imageUrl != null) {
            AsyncImage(
                model = card.imageUrl,
                contentDescription = card.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        Row {
            // Priority indicator bar
            if (priorityColor != null) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(if (card.description != null) 88.dp else 64.dp)
                        .background(priorityColor),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
            ) {
                // Labels
                if (card.labels.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        card.labels.forEach { label ->
                            LabelChip(label = label)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Title
                BasicText(
                    text = card.title,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        lineHeight = 18.sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Description
                val description = card.description
                if (description != null) {
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        text = description,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = colors.textTertiary,
                            lineHeight = 16.sp,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Priority badge (for non-NONE priorities)
                if (card.priority != CardPriority.NONE) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(priorityColor!!, CircleShape),
                        )
                        Spacer(Modifier.width(6.dp))
                        BasicText(
                            text = card.priority.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = priorityColor,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LabelChip(label: CardLabel) {
    val colors = KankanTheme.colors

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(label.color.copy(alpha = if (colors.isDark) 0.25f else 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        BasicText(
            text = label.name,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (colors.isDark) {
                    label.color.copy(alpha = 0.9f)
                } else {
                    label.color.copy(alpha = 0.8f)
                },
            ),
        )
    }
}

/**
 * A compact themed card with minimal info.
 * Shows title and label dots only.
 */
@Composable
fun SimpleKCardScope.ThemedCompactCard(
    title: String,
    labels: List<CardLabel> = emptyList(),
    priority: CardPriority = CardPriority.NONE,
    modifier: Modifier = Modifier,
) {
    val colors = KankanTheme.colors

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 1.dp,
        animationSpec = spring(),
        label = "elevation",
    )

    val priorityColor = when (priority) {
        CardPriority.NONE -> null
        CardPriority.LOW -> PriorityColors.Low
        CardPriority.MEDIUM -> PriorityColors.Medium
        CardPriority.HIGH -> PriorityColors.High
        CardPriority.URGENT -> PriorityColors.Urgent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(colors.card)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Priority dot
        if (priorityColor != null) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(priorityColor, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
        }

        // Title
        BasicText(
            text = title,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        // Label dots
        if (labels.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                labels.take(3).forEach { label ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(label.color, CircleShape),
                    )
                }
            }
        }
    }
}
