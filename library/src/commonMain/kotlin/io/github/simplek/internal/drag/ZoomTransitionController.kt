package io.github.simplek.internal.drag

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.simplek.state.SimpleKState
import io.github.simplek.state.ZoomPhase
import io.github.simplek.model.SimpleKItem

/**
 * Controls the zoom-out transition during card drag operations.
 *
 * The zoom transition provides a bird's-eye view of all columns when
 * dragging a card outside its source column, making cross-column moves easier.
 */
internal class ZoomTransitionController<T : SimpleKItem>(
    private val state: SimpleKState<T>,
    private val config: DragConfiguration,
) {

    /**
     * Check if zoom-out should be triggered and start the transition if needed.
     *
     * Zoom is triggered when:
     * - Zoom-out drag is enabled in config
     * - Currently in normal (non-zoomed) state
     * - Current drag position has left the source column bounds horizontally
     *
     * @param currentPosition Current drag position in root coordinates
     * @param sourceColumnBounds Bounds of the column where the drag started
     */
    fun checkAndTriggerZoomOut(
        currentPosition: Offset,
        sourceColumnBounds: Rect?,
    ) {
        if (!config.enableZoomOutDrag) return
        if (state.zoomPhase != ZoomPhase.Normal) return

        sourceColumnBounds ?: return

        val hasLeftColumn = currentPosition.x < sourceColumnBounds.left ||
            currentPosition.x > sourceColumnBounds.right

        if (hasLeftColumn) {
            state.startZoomOutTransition(config.zoomOutDurationMillis)
        }
    }

    /**
     * Start zoom-in transition to return to normal view.
     *
     * Should be called when drag ends while zoomed out.
     */
    fun triggerZoomIn() {
        if (!config.enableZoomOutDrag) return

        val phase = state.zoomPhase
        if (phase == ZoomPhase.ZoomingOut || phase == ZoomPhase.Miniature) {
            state.startZoomInTransition(config.zoomInDurationMillis)
        }
    }

    /**
     * Safely reset zoom state if drag ends unexpectedly.
     *
     * This ensures zoom doesn't get stuck in a transitioning state
     * if an error occurs during drag handling.
     */
    fun ensureZoomReset() {
        if (!config.enableZoomOutDrag) return

        val phase = state.zoomPhase
        if (phase == ZoomPhase.ZoomingOut || phase == ZoomPhase.Miniature) {
            state.startZoomInTransition(config.zoomInDurationMillis)
        }
    }

    /**
     * Check if we're currently in zoomed mode (miniature view).
     */
    val isZoomed: Boolean
        get() = state.zoomPhase != ZoomPhase.Normal

    /**
     * Check if we're in a stable zoomed state (not transitioning).
     */
    val isInMiniatureMode: Boolean
        get() = state.zoomPhase == ZoomPhase.Miniature
}

/**
 * Factory function to create a ZoomTransitionController from SimpleKConfig.
 */
internal fun <T : SimpleKItem> createZoomController(
    state: SimpleKState<T>,
    enableZoomOutDrag: Boolean,
    zoomOutDurationMillis: Long,
    zoomInDurationMillis: Long,
): ZoomTransitionController<T> {
    return ZoomTransitionController(
        state = state,
        config = DragConfiguration(
            enableCardDrag = true,
            enableZoomOutDrag = enableZoomOutDrag,
            zoomOutDurationMillis = zoomOutDurationMillis,
            zoomInDurationMillis = zoomInDurationMillis,
        ),
    )
}
