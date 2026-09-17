package com.t3code.explorer.domain.util

import kotlin.math.abs

enum class GestureAction { NONE, SEEK, BRIGHTNESS, VOLUME, DOUBLE_TAP_FORWARD, DOUBLE_TAP_BACKWARD, TOGGLE_PLAYBACK }

enum class GestureZone { LEFT, CENTER, RIGHT }

/**
 * Tuning for the video gesture layer.
 *
 * @param touchSlopPx minimum travel before a drag is considered at all. The UI passes the platform
 *                    value (`ViewConfiguration.scaledTouchSlop`), so the threshold follows density.
 * @param axisDominance how much stronger the dominant axis must be before a direction is locked.
 *                      Values above 1 keep diagonal movement undecided instead of firing two
 *                      gesture systems at the same time.
 * @param seekSpanMs how much a full width horizontal swipe seeks.
 * @param edgeFraction width of the double tap rewind/forward zones on each side.
 */
data class GestureConfig(
    val touchSlopPx: Float = 24f,
    val axisDominance: Float = 1.6f,
    val seekSpanMs: Long = 120_000L,
    val edgeFraction: Float = 0.28f
)

/** A gesture update. [fraction] is the travelled part of the axis, from -1 to 1. */
data class GestureUpdate(val action: GestureAction, val fraction: Float, val justLocked: Boolean)

/**
 * Direction locking gesture arbiter for the video player.
 *
 * A drag stays undecided until the pointer has travelled further than the touch slop *and* one axis
 * clearly dominates the other. From that moment the gesture is locked: the other axis is ignored
 * completely, so a horizontal seek can never leak into the volume or brightness system. The reported
 * [GestureUpdate.fraction] is measured from the start of the drag, which lets the UI apply absolute
 * values (position/volume/brightness captured when the gesture locked) instead of accumulating
 * rounding errors from every pointer event.
 *
 * Pure Kotlin: no Android or Compose dependency, so the arbitration is covered by unit tests.
 */
class VideoGestureController(private val config: GestureConfig = GestureConfig()) {
    private var totalDx = 0f
    private var totalDy = 0f
    private var width = 1f
    private var height = 1f
    private var zone = GestureZone.CENTER
    private var locked = GestureAction.NONE

    val isLocked: Boolean get() = locked != GestureAction.NONE
    val action: GestureAction get() = locked
    val seekSpanMs: Long get() = config.seekSpanMs

    fun onDragStart(x: Float, width: Float, height: Float) {
        totalDx = 0f
        totalDy = 0f
        this.width = width.coerceAtLeast(1f)
        this.height = height.coerceAtLeast(1f)
        zone = zoneFor(x, width)
        locked = GestureAction.NONE
    }

    /** Returns null while the gesture is below the touch slop or still ambiguous. */
    fun onDrag(dx: Float, dy: Float): GestureUpdate? {
        totalDx += dx
        totalDy += dy
        if (locked == GestureAction.NONE) {
            if (maxOf(abs(totalDx), abs(totalDy)) < config.touchSlopPx) return null
            val horizontal = abs(totalDx) >= abs(totalDy) * config.axisDominance
            val vertical = abs(totalDy) >= abs(totalDx) * config.axisDominance
            if (!horizontal && !vertical) return null
            locked = when {
                horizontal -> GestureAction.SEEK
                zone == GestureZone.LEFT -> GestureAction.BRIGHTNESS
                else -> GestureAction.VOLUME
            }
            return GestureUpdate(locked, fraction(), justLocked = true)
        }
        return GestureUpdate(locked, fraction(), justLocked = false)
    }

    /** Ends the drag and returns the action it was performing, so the UI can clear its indicator. */
    fun onDragEnd(): GestureAction {
        val finished = locked
        reset()
        return finished
    }

    fun onDragCancel(): GestureAction = onDragEnd()

    /** Milliseconds to add to the position captured when the gesture locked. */
    fun seekDeltaMs(fraction: Float): Long = (fraction.coerceIn(-1f, 1f) * config.seekSpanMs).toLong()

    fun zoneFor(x: Float, width: Float): GestureZone {
        val usable = width.coerceAtLeast(1f)
        val edge = usable * config.edgeFraction
        return when {
            x < edge -> GestureZone.LEFT
            x > usable - edge -> GestureZone.RIGHT
            else -> GestureZone.CENTER
        }
    }

    /** Double tap on the edges seeks; a double tap in the middle toggles playback. */
    fun doubleTapAction(x: Float, width: Float): GestureAction = when (zoneFor(x, width)) {
        GestureZone.LEFT -> GestureAction.DOUBLE_TAP_BACKWARD
        GestureZone.RIGHT -> GestureAction.DOUBLE_TAP_FORWARD
        GestureZone.CENTER -> GestureAction.TOGGLE_PLAYBACK
    }

    private fun fraction(): Float = when (locked) {
        GestureAction.SEEK -> (totalDx / width).coerceIn(-1f, 1f)
        GestureAction.BRIGHTNESS, GestureAction.VOLUME -> (-totalDy / height).coerceIn(-1f, 1f)
        else -> 0f
    }

    private fun reset() {
        totalDx = 0f
        totalDy = 0f
        locked = GestureAction.NONE
    }
}
