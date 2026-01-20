package io.github.simplek.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.simplek.LocalSimpleKTypography
import io.github.simplek.scope.SimpleKHeaderScope

/**
 * Default column header component.
 * Displays title and item count, with optional WIP limit indicator.
 */
@Composable
fun SimpleKHeaderScope.DefaultColumnHeader(
    title: String,
    accentColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val typography = LocalSimpleKTypography.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (accentColor != null) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                } else {
                    Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Title
        BasicText(
            text = title,
            style = typography.columnHeader.copy(
                color = accentColor ?: Color(0xFF374151),
            ),
        )

        // Count badge
        CountBadge(
            count = itemCount,
            maxCount = maxItems,
            isAtLimit = isAtLimit,
            accentColor = accentColor,
        )
    }
}

@Composable
private fun CountBadge(
    count: Int,
    maxCount: Int?,
    isAtLimit: Boolean,
    accentColor: Color?,
) {
    val backgroundColor = when {
        isAtLimit -> Color(0xFFEF4444).copy(alpha = 0.1f)
        maxCount != null -> Color(0xFF6B7280).copy(alpha = 0.1f)
        else -> accentColor?.copy(alpha = 0.15f) ?: Color(0xFF6B7280).copy(alpha = 0.1f)
    }

    val textColor = when {
        isAtLimit -> Color(0xFFEF4444)
        else -> accentColor ?: Color(0xFF6B7280)
    }

    val text = if (maxCount != null) "$count/$maxCount" else count.toString()

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
            ),
        )
    }
}

/**
 * Minimal column header with just a title and count dot.
 */
@Composable
fun SimpleKHeaderScope.MinimalColumnHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    val typography = LocalSimpleKTypography.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = title,
            style = typography.columnHeader.copy(
                color = Color(0xFF374151),
            ),
        )

        // Count dot
        if (itemCount > 0) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = itemCount.toString(),
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7280),
                    ),
                )
            }
        }
    }
}
