package io.github.kankan.demo.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Color palette for the Kankan demo app.
 * Two distinct themes: Warm Editorial (light) and Midnight Studio (dark).
 */
@Immutable
data class KankanColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val card: Color,
    val cardElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentAlt: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val border: Color,
    val borderSubtle: Color,
    val divider: Color,
    val isDark: Boolean,
) {
    companion object {
        /**
         * Warm Editorial - Light theme
         * Warm off-whites with burnt orange accent
         */
        val Light = KankanColors(
            background = Color(0xFFFAF9F7),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF5F5F3),
            card = Color(0xFFFFFFFF),
            cardElevated = Color(0xFFFFFFFF),
            textPrimary = Color(0xFF1A1A1A),
            textSecondary = Color(0xFF6B7280),
            textTertiary = Color(0xFF9CA3AF),
            accent = Color(0xFFE85D04),
            accentAlt = Color(0xFF0EA5E9),
            success = Color(0xFF059669),
            warning = Color(0xFFF59E0B),
            error = Color(0xFFDC2626),
            border = Color(0xFFE5E7EB),
            borderSubtle = Color(0xFFF3F4F6),
            divider = Color(0xFFE5E7EB),
            isDark = false,
        )

        /**
         * Midnight Studio - Dark theme
         * Deep blacks with cyan accent
         */
        val Dark = KankanColors(
            background = Color(0xFF0F0F0F),
            surface = Color(0xFF1A1A1A),
            surfaceVariant = Color(0xFF242424),
            card = Color(0xFF1E1E1E),
            cardElevated = Color(0xFF262626),
            textPrimary = Color(0xFFFAFAFA),
            textSecondary = Color(0xFF9CA3AF),
            textTertiary = Color(0xFF6B7280),
            accent = Color(0xFF22D3EE),
            accentAlt = Color(0xFFF472B6),
            success = Color(0xFF34D399),
            warning = Color(0xFFFBBF24),
            error = Color(0xFFF87171),
            border = Color(0xFF2D2D2D),
            borderSubtle = Color(0xFF1F1F1F),
            divider = Color(0xFF2D2D2D),
            isDark = true,
        )
    }
}

val LocalKankanColors = staticCompositionLocalOf { KankanColors.Light }

/**
 * Priority colors - consistent across themes
 */
object PriorityColors {
    val None = Color(0xFF9CA3AF)
    val Low = Color(0xFF22C55E)
    val Medium = Color(0xFFF59E0B)
    val High = Color(0xFFEF4444)
    val Urgent = Color(0xFF7C3AED)
}

/**
 * Label colors for cards
 */
object LabelPresets {
    val Design = Color(0xFF8B5CF6)
    val Development = Color(0xFF3B82F6)
    val Marketing = Color(0xFFF472B6)
    val QA = Color(0xFF14B8A6)
    val Documentation = Color(0xFF6366F1)
    val Bug = Color(0xFFEF4444)
    val Feature = Color(0xFF22C55E)
    val Enhancement = Color(0xFF0EA5E9)
    val Urgent = Color(0xFFF97316)
}
