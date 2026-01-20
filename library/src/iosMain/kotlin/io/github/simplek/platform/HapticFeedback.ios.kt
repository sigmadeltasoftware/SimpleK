package io.github.simplek.platform

/**
 * iOS Haptic Feedback Support Documentation
 * =========================================
 *
 * On iOS, haptic feedback is provided automatically by Compose Multiplatform through
 * `LocalHapticFeedback.current`. The implementation uses `UIImpactFeedbackGenerator`
 * internally and works without any additional platform-specific code.
 *
 * Haptic Feedback Types Used in Kankan:
 * -------------------------------------
 * - HapticFeedbackType.LongPress: Triggered when a card drag starts (after long press)
 * - HapticFeedbackType.TextHandleMove: Triggered when:
 *   - A card moves to a new position during drag
 *   - Hovering over a mini column in the zoom-out overlay
 *   - Tapping the chevron button to move a card
 *
 * iOS Haptic Requirements:
 * ------------------------
 * - Requires iPhone 7 or later (devices with Taptic Engine)
 * - User must have haptics enabled in Settings > Sounds & Haptics > System Haptics
 * - Works on both physical devices and newer iOS Simulator versions
 *
 * Configuration:
 * --------------
 * Haptic feedback can be disabled via KanbanConfig:
 *   KanbanConfig(enableHapticFeedback = false)
 *
 * Testing:
 * --------
 * To verify iOS haptic feedback is working:
 * 1. Run the demo app on a physical iOS device
 * 2. Long-press a card to initiate drag - should feel a "thud" feedback
 * 3. Move the card to a new position - should feel subtle "tick" feedback
 * 4. Drag outside column to trigger zoom-out and hover over mini columns
 * 5. Tap the chevron (>) button on a card - should feel "tick" feedback
 *
 * Note: Haptic feedback intensity cannot be customized on iOS through Compose
 * Multiplatform's HapticFeedback API. The system uses predefined intensity levels.
 */

// No actual code is needed - this file serves as documentation
// Compose Multiplatform handles iOS haptics automatically via LocalHapticFeedback
