package com.t3code.explorer

import com.t3code.explorer.domain.util.FileOperationPolicy
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileOperationPolicyTest {
    @Test fun `unsafe names cannot escape the selected parent`() {
        assertEquals(".._", FileOperationPolicy.safeName("../"))
        assertEquals("untitled", FileOperationPolicy.safeName(".."))
        assertEquals("hello_world.txt", FileOperationPolicy.safeName("hello/world.txt"))
    }

    @Test fun `conflict name keeps extension and never overwrites`() {
        val root = Files.createTempDirectory("conflicts").toFile()
        val original = root.resolve("photo.jpg").apply { writeText("one") }
        val second = FileOperationPolicy.conflictSafe(original)
        second.writeText("two")
        assertEquals("photo (1).jpg", second.name)
        assertEquals("one", original.readText())
        root.deleteRecursively()
    }

    @Test fun `directory containment uses canonical paths`() {
        val root = Files.createTempDirectory("containment").toFile()
        val child = root.resolve("child").apply { mkdirs() }
        assertTrue(FileOperationPolicy.isInsideOrEqual(root, child))
        assertTrue(FileOperationPolicy.isInsideOrEqual(root, root))
        assertFalse(FileOperationPolicy.isInsideOrEqual(child, root))
        root.deleteRecursively()
    }
}
