package io.github.simplek.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.simplek.LocalBoardTheme
import io.github.simplek.LocalCardCornerRadius
import io.github.simplek.LocalSimpleKTypography
import io.github.simplek.scope.SimpleKCardScope

/**
 * Simple card with title only.
 * Minimal styling for basic use cases.
 *
 * Uses the board theme from [LocalBoardTheme] for colors,
 * typography from [LocalSimpleKTypography], and corner radius from [LocalCardCornerRadius].
 *
 * @param title The card title text
 * @param modifier Optional modifier
 * @param backgroundColor Override the background color (defaults to theme's card background)
 * @param titleColor Override the title color (defaults to theme-appropriate text color)
 * @param cornerRadius Override the corner radius (defaults to [LocalCardCornerRadius])
 */
@Composable
fun SimpleKCardScope.SimpleCard(
    title: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    titleColor: Color? = null,
    cornerRadius: Dp? = null,
) {
    val theme = LocalBoardTheme.current
    val typography = LocalSimpleKTypography.current
    val defaultCornerRadius = LocalCardCornerRadius.current

    val elevation = if (isDragging) 8.dp else 2.dp
    val cardShape = RoundedCornerShape(cornerRadius ?: defaultCornerRadius)
    val cardBackground = backgroundColor ?: theme.cardBackground
    // Use a dark text color for light backgrounds, light for dark backgrounds
    val textColor = titleColor ?: if (cardBackground == Color.White || theme.cardBackground == Color.White) {
        Color(0xFF1A1A1A)
    } else {
        Color(0xFFF9FAFB)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, cardShape)
            .background(cardBackground, cardShape)
            .padding(12.dp),
    ) {
        BasicText(
            text = title,
            style = typography.cardTitle.copy(color = textColor),
        )
    }
}
