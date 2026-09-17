package com.t3code.explorer

import com.t3code.explorer.domain.util.GestureAction
import com.t3code.explorer.domain.util.VideoGestureDecider
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoGestureDeciderTest {
    private val decider = VideoGestureDecider()

    @Test fun `small movement is ignored`() = assertEquals(GestureAction.NONE, decider.decide(5f, 5f, 1000f, true).action)
    @Test fun `horizontal movement seeks`() = assertEquals(GestureAction.SEEK, decider.decide(180f, 20f, 1000f, true).action)
    @Test fun `left vertical movement changes brightness`() = assertEquals(GestureAction.BRIGHTNESS, decider.decide(10f, -180f, 1000f, true).action)
    @Test fun `right vertical movement changes volume`() = assertEquals(GestureAction.VOLUME, decider.decide(10f, -180f, 1000f, false).action)
    @Test fun `double tap direction is explicit`() = assertEquals(GestureAction.DOUBLE_TAP_FORWARD, decider.doubleTap(200f).action)
    @Test fun `ambiguous diagonal movement is ignored`() = assertEquals(GestureAction.NONE, decider.decide(100f, 90f, 1000f, true).action)
    @Test fun `zero width never produces invalid seek amount`() = assertEquals(1f, decider.decide(100f, 0f, 0f, true).amount)
}
