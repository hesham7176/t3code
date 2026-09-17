package com.t3code.explorer.domain.util

import kotlin.math.abs

enum class GestureAction { NONE, SEEK, BRIGHTNESS, VOLUME, DOUBLE_TAP_FORWARD, DOUBLE_TAP_BACKWARD, TOGGLE_PLAYBACK }

data class GestureDecision(
    val action: GestureAction,
    val amount: Float = 0f
)

/** Pure gesture policy: direction is locked once touch slop is exceeded. */
class VideoGestureDecider(
    private val touchSlop: Float = 24f,
    private val horizontalBias: Float = 1.15f,
    private val doubleTapDistance: Float = 120f
) {
    fun decide(dx: Float, dy: Float, width: Float, isLeftSide: Boolean): GestureDecision {
        val distance = maxOf(abs(dx), abs(dy))
        if (distance < touchSlop) return GestureDecision(GestureAction.NONE)
        return if (abs(dx) >= abs(dy) * horizontalBias) {
            GestureDecision(GestureAction.SEEK, (dx / width.coerceAtLeast(1f)).coerceIn(-1f, 1f))
        } else if (abs(dy) > abs(dx)) {
            val amount = (-dy / 600f).coerceIn(-1f, 1f)
            GestureDecision(if (isLeftSide) GestureAction.BRIGHTNESS else GestureAction.VOLUME, amount)
        } else {
            GestureDecision(GestureAction.NONE)
        }
    }

    fun doubleTap(xDelta: Float): GestureDecision = when {
        abs(xDelta) < doubleTapDistance -> GestureDecision(GestureAction.TOGGLE_PLAYBACK)
        xDelta > 0 -> GestureDecision(GestureAction.DOUBLE_TAP_FORWARD)
        else -> GestureDecision(GestureAction.DOUBLE_TAP_BACKWARD)
    }
}
