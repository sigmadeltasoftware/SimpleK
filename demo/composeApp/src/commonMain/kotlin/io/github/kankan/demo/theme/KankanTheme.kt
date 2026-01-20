package io.github.kankan.demo.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * Main theme composable for the Kankan demo app.
 * Provides animated color transitions between light and dark themes.
 */
@Composable
fun KankanTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val targetColors = if (isDarkTheme) KankanColors.Dark else KankanColors.Light

    // Animate all colors for smooth theme transitions
    val animationSpec = spring<Color>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    val background by animateColorAsState(targetColors.background, animationSpec, label = "bg")
    val surface by animateColorAsState(targetColors.surface, animationSpec, label = "surface")
    val surfaceVariant by animateColorAsState(targetColors.surfaceVariant, animationSpec, label = "surfaceVariant")
    val card by animateColorAsState(targetColors.card, animationSpec, label = "card")
    val cardElevated by animateColorAsState(targetColors.cardElevated, animationSpec, label = "cardElevated")
    val textPrimary by animateColorAsState(targetColors.textPrimary, animationSpec, label = "textPrimary")
    val textSecondary by animateColorAsState(targetColors.textSecondary, animationSpec, label = "textSecondary")
    val textTertiary by animateColorAsState(targetColors.textTertiary, animationSpec, label = "textTertiary")
    val accent by animateColorAsState(targetColors.accent, animationSpec, label = "accent")
    val accentAlt by animateColorAsState(targetColors.accentAlt, animationSpec, label = "accentAlt")
    val success by animateColorAsState(targetColors.success, animationSpec, label = "success")
    val warning by animateColorAsState(targetColors.warning, animationSpec, label = "warning")
    val error by animateColorAsState(targetColors.error, animationSpec, label = "error")
    val border by animateColorAsState(targetColors.border, animationSpec, label = "border")
    val borderSubtle by animateColorAsState(targetColors.borderSubtle, animationSpec, label = "borderSubtle")
    val divider by animateColorAsState(targetColors.divider, animationSpec, label = "divider")

    val animatedColors = remember(isDarkTheme) {
        KankanColors(
            background = background,
            surface = surface,
            surfaceVariant = surfaceVariant,
            card = card,
            cardElevated = cardElevated,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            textTertiary = textTertiary,
            accent = accent,
            accentAlt = accentAlt,
            success = success,
            warning = warning,
            error = error,
            border = border,
            borderSubtle = borderSubtle,
            divider = divider,
            isDark = isDarkTheme,
        )
    }.copy(
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        card = card,
        cardElevated = cardElevated,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textTertiary = textTertiary,
        accent = accent,
        accentAlt = accentAlt,
        success = success,
        warning = warning,
        error = error,
        border = border,
        borderSubtle = borderSubtle,
        divider = divider,
    )

    CompositionLocalProvider(
        LocalKankanColors provides animatedColors,
        content = content,
    )
}

/**
 * Access the current theme colors.
 */
object KankanTheme {
    val colors: KankanColors
        @Composable
        get() = LocalKankanColors.current
}
