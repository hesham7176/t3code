package com.t3code.explorer

import com.t3code.explorer.domain.util.SafNaming
import kotlin.test.Test
import kotlin.test.assertEquals

class SafNamingTest {
    @Test fun `unsafe characters are replaced but spaces survive`() {
        assertEquals("My Holiday.mp4", SafNaming.safeName("My Holiday.mp4"))
        assertEquals("a_b.txt", SafNaming.safeName("a/b.txt"))
        assertEquals("_", SafNaming.safeName("/"))
        assertEquals("untitled", SafNaming.safeName(".."))
    }

    @Test fun `file conflicts keep the extension`() {
        val taken = mutableSetOf("photo.jpg")
        assertEquals("photo (1).jpg", SafNaming.conflictName("photo.jpg", false) { taken.contains(it) })
        assertEquals("photo (3).jpg", SafNaming.conflictName("photo.jpg", false) { it != "photo (3).jpg" })
    }

    @Test fun `directory conflicts keep dots that are not extensions`() {
        val taken = mutableSetOf("Season 1.5")
        assertEquals("Season 1.5 (1)", SafNaming.conflictName("Season 1.5", true) { taken.contains(it) })
        assertEquals("Season 1 (1).5", SafNaming.conflictName("Season 1.5", false) { taken.contains(it) })
    }

    @Test fun `a leading dot is not treated as an extension`() {
        val taken = mutableSetOf(".gitignore")
        assertEquals(".gitignore (1)", SafNaming.conflictName(".gitignore", false) { taken.contains(it) })
    }

    @Test fun `a free name is used unchanged`() {
        assertEquals("fresh.txt", SafNaming.conflictName("fresh.txt", false) { false })
    }
}
