package com.t3code.explorer

import com.t3code.explorer.domain.util.GestureAction
import com.t3code.explorer.domain.util.GestureConfig
import com.t3code.explorer.domain.util.GestureZone
import com.t3code.explorer.domain.util.VideoGestureController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoGestureControllerTest {
    private val config = GestureConfig(touchSlopPx = 24f, axisDominance = 1.6f, seekSpanMs = 120_000L, edgeFraction = 0.28f)
    private fun controller() = VideoGestureController(config)
    private fun VideoGestureController.start(x: Float, width: Float = 1000f, height: Float = 600f) = onDragStart(x, width, height)

    @Test fun `movement below the touch slop never starts a gesture`() {
        val controller = controller().apply { start(500f) }
        assertNull(controller.onDrag(10f, 10f))
        assertNull(controller.onDrag(-13f, -6f))
        assertFalse(controller.isLocked)
        assertEquals(GestureAction.NONE, controller.action)
    }

    @Test fun `horizontal travel locks seek and scales with the width`() {
        val controller = controller().apply { start(500f) }
        val first = controller.onDrag(60f, 4f)!!
        assertEquals(GestureAction.SEEK, first.action)
        assertTrue(first.justLocked)
        val second = controller.onDrag(190f, 2f)!!
        assertEquals(GestureAction.SEEK, second.action)
        assertFalse(second.justLocked)
        assertEquals(0.25f, second.fraction, 0.001f)
    }

    @Test fun `left vertical travel locks brightness and right vertical travel locks volume`() {
        val left = controller().apply { start(80f) }
        assertEquals(GestureAction.BRIGHTNESS, left.onDrag(2f, -80f)!!.action)
        val right = controller().apply { start(920f) }
        assertEquals(GestureAction.VOLUME, right.onDrag(2f, -80f)!!.action)
        val centre = controller().apply { start(500f) }
        assertEquals(GestureAction.VOLUME, centre.onDrag(0f, -80f)!!.action)
    }

    @Test fun `vertical fraction is positive when the finger moves up`() {
        val controller = controller().apply { start(920f) }
        val update = controller.onDrag(0f, -300f)!!
        assertEquals(0.5f, update.fraction, 0.001f)
    }

    @Test fun `an ambiguous diagonal never locks even after a long travel`() {
        val controller = controller().apply { start(500f) }
        repeat(20) { assertNull(controller.onDrag(20f, 20f)) }
        assertFalse(controller.isLocked)
        // The movement only becomes a gesture once one axis clearly dominates.
        val update = controller.onDrag(400f, 0f)
        assertEquals(GestureAction.SEEK, update?.action)
    }

    @Test fun `a locked gesture ignores the other axis completely`() {
        val controller = controller().apply { start(920f) }
        controller.onDrag(0f, -60f)
        assertEquals(GestureAction.VOLUME, controller.action)
        val update = controller.onDrag(400f, 5f)!!
        assertEquals(GestureAction.VOLUME, update.action)
        assertEquals(0.09166667f, update.fraction, 0.001f)
    }

    @Test fun `a new drag starts undecided again`() {
        val controller = controller().apply { start(500f) }
        controller.onDrag(120f, 0f)
        assertEquals(GestureAction.NONE, controller.onDragEnd())
        assertFalse(controller.isLocked)
        controller.start(80f)
        assertNull(controller.onDrag(3f, 3f))
        assertEquals(GestureAction.BRIGHTNESS, controller.onDrag(0f, -50f)!!.action)
    }

    @Test fun `cancelling a drag reports the action and resets the controller`() {
        val controller = controller().apply { start(500f) }
        controller.onDrag(-90f, 0f)
        assertEquals(GestureAction.SEEK, controller.onDragCancel())
        assertFalse(controller.isLocked)
    }

    @Test fun `seek delta follows the configured span and stays bounded`() {
        val controller = controller().apply { start(500f) }
        assertEquals(30_000L, controller.seekDeltaMs(0.25f))
        assertEquals(-60_000L, controller.seekDeltaMs(-0.5f))
        assertEquals(120_000L, controller.seekDeltaMs(4f))
        assertEquals(-120_000L, controller.seekDeltaMs(-4f))
    }

    @Test fun `double tap zones rewind, forward and toggle`() {
        val controller = controller()
        assertEquals(GestureAction.DOUBLE_TAP_BACKWARD, controller.doubleTapAction(40f, 1000f))
        assertEquals(GestureAction.TOGGLE_PLAYBACK, controller.doubleTapAction(500f, 1000f))
        assertEquals(GestureAction.DOUBLE_TAP_FORWARD, controller.doubleTapAction(980f, 1000f))
        assertEquals(GestureZone.LEFT, controller.zoneFor(0f, 1000f))
        assertEquals(GestureZone.CENTER, controller.zoneFor(500f, 1000f))
        assertEquals(GestureZone.RIGHT, controller.zoneFor(999f, 1000f))
    }

    @Test fun `a zero sized surface never produces invalid values`() {
        val controller = controller().apply { start(0f, width = 0f, height = 0f) }
        controller.onDrag(50f, 0f)
        val update = controller.onDrag(50f, 0f)
        assertTrue(update == null || update.fraction in -1f..1f)
        assertEquals(0L, controller.seekDeltaMs(Float.NaN))
    }
}
