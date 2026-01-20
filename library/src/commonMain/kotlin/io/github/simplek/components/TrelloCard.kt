package io.github.simplek.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.simplek.LocalBoardTheme
import io.github.simplek.LocalCardCornerRadius
import io.github.simplek.LocalSimpleKTypography
import io.github.simplek.model.CardLabel
import io.github.simplek.model.CardPriority
import io.github.simplek.model.DefaultCard
import io.github.simplek.scope.SimpleKCardScope

/**
 * Full-featured Trello-style card.
 * Displays title, description, labels, and priority indicator.
 *
 * Uses the board theme from [LocalBoardTheme] for colors,
 * typography from [LocalSimpleKTypography], and corner radius from [LocalCardCornerRadius].
 *
 * @param card The DefaultCard data to display
 * @param modifier Optional modifier
 * @param backgroundColor Override the background color (defaults to theme's card background)
 * @param titleColor Override the title color (defaults to theme-appropriate text color)
 * @param subtitleColor Override the subtitle/description color
 * @param cornerRadius Override the corner radius (defaults to [LocalCardCornerRadius])
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimpleKCardScope.TrelloCard(
    card: DefaultCard,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    titleColor: Color? = null,
    subtitleColor: Color? = null,
    cornerRadius: Dp? = null,
) {
    val theme = LocalBoardTheme.current
    val typography = LocalSimpleKTypography.current
    val defaultCornerRadius = LocalCardCornerRadius.current

    val elevation = if (isDragging) 8.dp else 2.dp
    val cardShape = RoundedCornerShape(cornerRadius ?: defaultCornerRadius)
    val cardBackground = backgroundColor ?: theme.cardBackground
    // Use a dark text color for light backgrounds, light for dark backgrounds
    val isLightBackground = cardBackground == Color.White || theme.cardBackground == Color.White
    val textColor = titleColor ?: if (isLightBackground) Color(0xFF1A1A1A) else Color(0xFFF9FAFB)
    val descriptionColor = subtitleColor ?: if (isLightBackground) Color(0xFF6B7280) else Color(0xFF9CA3AF)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, cardShape)
            .background(cardBackground, cardShape)
            .then(
                if (card.priority != CardPriority.NONE) {
                    Modifier.drawPriorityIndicator(card.priority, cornerRadius ?: defaultCornerRadius)
                } else {
                    Modifier
                }
            )
            .padding(12.dp),
    ) {
        // Labels
        if (card.labels.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                card.labels.forEach { label ->
                    LabelChip(label)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Title
        BasicText(
            text = card.title,
            style = typography.cardTitle.copy(color = textColor),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        // Description
        if (!card.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            BasicText(
                text = card.description,
                style = typography.cardSubtitle.copy(color = descriptionColor),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LabelChip(label: CardLabel) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(label.color.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        BasicText(
            text = label.name,
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = label.color,
            ),
        )
    }
}

private fun Modifier.drawPriorityIndicator(priority: CardPriority, cornerRadius: Dp): Modifier {
    val color = when (priority) {
        CardPriority.LOW -> Color(0xFF22C55E)
        CardPriority.MEDIUM -> Color(0xFFF59E0B)
        CardPriority.HIGH -> Color(0xFFEF4444)
        CardPriority.URGENT -> Color(0xFF7C3AED)
        CardPriority.NONE -> Color.Transparent
    }

    return this.then(
        Modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(color.copy(alpha = 0.1f))
    )
}

/**
 * Compact card for dense boards.
 * Shows only title and labels in a minimal format.
 *
 * Uses the board theme from [LocalBoardTheme] for colors,
 * typography from [LocalSimpleKTypography], and corner radius from [LocalCardCornerRadius].
 *
 * @param title The card title text
 * @param labels Optional list of labels to display as colored dots
 * @param modifier Optional modifier
 * @param backgroundColor Override the background color (defaults to theme's card background)
 * @param titleColor Override the title color (defaults to theme-appropriate text color)
 * @param cornerRadius Override the corner radius (defaults to a compact 6.dp)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimpleKCardScope.CompactCard(
    title: String,
    labels: List<CardLabel> = emptyList(),
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    titleColor: Color? = null,
    cornerRadius: Dp? = null,
) {
    val theme = LocalBoardTheme.current
    val typography = LocalSimpleKTypography.current
    val defaultCornerRadius = LocalCardCornerRadius.current

    val elevation = if (isDragging) 6.dp else 1.dp
    val cardShape = RoundedCornerShape(cornerRadius ?: defaultCornerRadius)
    val cardBackground = backgroundColor ?: theme.cardBackground
    // Use a dark text color for light backgrounds, light for dark backgrounds
    val isLightBackground = cardBackground == Color.White || theme.cardBackground == Color.White
    val textColor = titleColor ?: if (isLightBackground) Color(0xFF374151) else Color(0xFFD1D5DB)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, cardShape)
            .background(cardBackground, cardShape)
            .padding(8.dp),
    ) {
        // Mini labels (just colored dots)
        if (labels.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                labels.take(4).forEach { label ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(label.color),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Title - use subtitle style for compact appearance
        BasicText(
            text = title,
            style = typography.cardSubtitle.copy(color = textColor),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
