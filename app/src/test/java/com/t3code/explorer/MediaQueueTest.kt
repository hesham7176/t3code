package com.t3code.explorer

import com.t3code.explorer.domain.util.MediaQueue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MediaQueueTest {
    private fun queue(repeat: MediaQueue.RepeatMode = MediaQueue.RepeatMode.OFF, shuffle: Boolean = false) =
        MediaQueue(listOf("a", "b", "c"), startIndex = 0, repeatMode = repeat, shuffleEnabled = shuffle)

    @Test fun `next and previous walk the queue and stop at the edges`() {
        val queue = queue()
        assertEquals("b", queue.advanceNext())
        assertEquals("c", queue.advanceNext())
        assertNull(queue.advanceNext())
        assertFalse(queue.hasNext())
        assertEquals("b", queue.advancePrevious())
        assertEquals("a", queue.advancePrevious())
        assertNull(queue.advancePrevious())
        assertFalse(queue.hasPrevious())
    }

    @Test fun `repeat all wraps in both directions`() {
        val queue = queue(repeat = MediaQueue.RepeatMode.ALL)
        assertEquals("b", queue.advanceNext())
        assertEquals("c", queue.advanceNext())
        assertEquals("a", queue.advanceNext())
        assertEquals("c", queue.advancePrevious())
        assertTrue(queue.hasNext())
    }

    @Test fun `repeat one still exposes the queue edges`() {
        val queue = queue(repeat = MediaQueue.RepeatMode.ONE)
        assertEquals("b", queue.advanceNext())
        assertEquals("c", queue.advanceNext())
        assertNull(queue.advanceNext())
    }

    @Test fun `shuffle never repeats the current item and always stays inside the queue`() {
        val queue = queue(shuffle = true)
        repeat(50) {
            val next = queue.advanceNext()
            assertTrue(next in setOf("a", "b", "c"))
            assertTrue(queue.currentIndex in 0..2)
        }
    }

    @Test fun `an empty queue has no navigation and no current item`() {
        val queue = MediaQueue<String>()
        assertTrue(queue.isEmpty)
        assertNull(queue.current())
        assertNull(queue.advanceNext())
        assertNull(queue.advancePrevious())
    }

    @Test fun `starting outside the range is clamped and moveTo ignores invalid indexes`() {
        val queue = MediaQueue(listOf("only"), startIndex = 7)
        assertEquals(0, queue.currentIndex)
        assertEquals("only", queue.current())
        queue.moveTo(4)
        assertEquals(0, queue.currentIndex)
        queue.moveTo(-1)
        assertEquals(0, queue.currentIndex)
    }

    @Test fun `indexOf finds the requested entry`() {
        assertEquals(2, queue().indexOf { it == "c" })
        assertEquals(-1, queue().indexOf { it == "missing" })
    }
}
