package io.github.simplek.internal

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Controller for automatic scrolling during drag operations.
 */
internal class AutoScrollController(
    private val coroutineScope: CoroutineScope,
) {
    private var verticalScrollJob: Job? = null
    private var horizontalScrollJob: Job? = null

    /**
     * Start auto-scrolling a column vertically.
     *
     * @param scrollState The lazy list state to scroll
     * @param speedPxPerSecond Scroll speed in pixels per second (negative for up)
     */
    fun startVerticalScroll(
        scrollState: LazyListState,
        speedPxPerSecond: Float,
    ) {
        verticalScrollJob?.cancel()
        verticalScrollJob = coroutineScope.launch {
            while (isActive) {
                val delta = (speedPxPerSecond / 60f) // 60fps
                scrollLazyListBy(scrollState, delta)
                delay(16) // ~60fps
            }
        }
    }

    /**
     * Stop vertical auto-scrolling.
     */
    fun stopVerticalScroll() {
        verticalScrollJob?.cancel()
        verticalScrollJob = null
    }

    /**
     * Start auto-scrolling the board horizontally.
     *
     * @param scrollState The scroll state to scroll
     * @param speedPxPerSecond Scroll speed in pixels per second (negative for left)
     */
    fun startHorizontalScroll(
        scrollState: ScrollState,
        speedPxPerSecond: Float,
    ) {
        horizontalScrollJob?.cancel()
        horizontalScrollJob = coroutineScope.launch {
            while (isActive) {
                val delta = (speedPxPerSecond / 60f).toInt()
                scrollState.animateScrollTo(scrollState.value + delta)
                delay(16) // ~60fps
            }
        }
    }

    /**
     * Stop horizontal auto-scrolling.
     */
    fun stopHorizontalScroll() {
        horizontalScrollJob?.cancel()
        horizontalScrollJob = null
    }

    /**
     * Stop all auto-scrolling.
     */
    fun stopAll() {
        stopVerticalScroll()
        stopHorizontalScroll()
    }

    /**
     * Calculate scroll speed based on distance from edge.
     * Uses an ease-in curve for gradual acceleration.
     *
     * @param distanceFromEdge How far into the scroll zone (0 = at edge, positive = deeper)
     * @param maxDistance Maximum scroll zone size
     * @param maxSpeed Maximum scroll speed
     * @return Scroll speed in pixels per second
     */
    fun calculateScrollSpeed(
        distanceFromEdge: Float,
        maxDistance: Float,
        maxSpeed: Float,
    ): Float {
        if (distanceFromEdge <= 0f || maxDistance <= 0f) return 0f
        val normalizedDistance = (distanceFromEdge / maxDistance).coerceIn(0f, 1f)
        // Ease-in curve: speed increases as you get deeper into the scroll zone
        return maxSpeed * normalizedDistance * normalizedDistance
    }
}

/**
 * Scroll a LazyListState by a pixel amount.
 * Uses scroll { scrollBy() } for smoother continuous scrolling instead of animateScrollToItem
 * which can cause choppy behavior by starting new animations each call.
 */
private suspend fun scrollLazyListBy(scrollState: LazyListState, pixels: Float) {
    if (pixels != 0f) {
        scrollState.scroll {
            scrollBy(pixels)
        }
    }
}
