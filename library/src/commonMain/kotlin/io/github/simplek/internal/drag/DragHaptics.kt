package io.github.simplek.internal.drag

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Provides haptic feedback for drag operations.
 */
internal object DragHaptics {

    /**
     * Perform haptic feedback when drag starts (long press detected).
     */
    fun onDragStart(hapticFeedback: HapticFeedback) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Perform haptic feedback when card position changes.
     */
    fun onPositionChange(hapticFeedback: HapticFeedback) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /**
     * Perform haptic feedback when hovering over a new column in zoom mode.
     */
    fun onColumnHover(hapticFeedback: HapticFeedback) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}
