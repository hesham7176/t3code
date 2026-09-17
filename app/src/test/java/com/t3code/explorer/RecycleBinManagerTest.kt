package com.t3code.explorer

import com.t3code.explorer.data.files.RecycleBinManager
import com.t3code.explorer.data.files.RecycleEntryRecord
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class RecycleBinManagerTest {
    private fun sandbox(name: String): java.io.File = Files.createTempDirectory(name).toFile()

    @Test fun `delete stores the original path and restore returns the content`() = runTest {
        val root = sandbox("bin-round-trip")
        val bin = root.resolve("bin")
        val manager = RecycleBinManager(bin)
        val original = root.resolve("Movies/episode 01.mkv").apply { parentFile.mkdirs(); writeText("payload") }
        manager.delete(listOf(original)).getOrThrow()
        assertFalse(original.exists())
        val entries = manager.entries()
        assertEquals(1, entries.size)
        assertEquals(original.absolutePath, entries.first().originalPath)
        assertEquals("episode 01.mkv", entries.first().name)
        assertEquals(7L, entries.first().size)
        val restored = manager.restore(RecycleEntryRecord(entries.first().originalPath, entries.first().deletedPath)).getOrThrow()
        assertEquals(original.absolutePath, restored.absolutePath)
        assertEquals("payload", restored.readText())
        assertTrue(manager.entries().isEmpty())
        root.deleteRecursively()
    }

    @Test fun `restore keeps both copies when the original path is taken again`() = runTest {
        val root = sandbox("bin-conflict")
        val bin = root.resolve("bin")
        val manager = RecycleBinManager(bin)
        val original = root.resolve("note.txt").apply { writeText("first") }
        manager.delete(listOf(original)).getOrThrow()
        root.resolve("note.txt").writeText("replacement")
        val entry = manager.entries().single()
        val restored = manager.restore(RecycleEntryRecord(entry.originalPath, entry.deletedPath)).getOrThrow()
        assertEquals(root.resolve("note (1).txt").absolutePath, restored.absolutePath)
        assertEquals("first", restored.readText())
        assertEquals("replacement", root.resolve("note.txt").readText())
        root.deleteRecursively()
    }

    @Test fun `restore refuses invalid or missing metadata instead of guessing a path`() = runTest {
        val root = sandbox("bin-metadata")
        val manager = RecycleBinManager(root.resolve("bin"))
        assertTrue(manager.restore(RecycleEntryRecord("", "/nowhere")).isFailure)
        assertTrue(manager.restore(RecycleEntryRecord("relative/path.txt", "/nowhere")).isFailure)
        assertTrue(manager.restore(RecycleEntryRecord("/absolute/path.txt", "/nowhere/deleted")).isFailure)
        root.deleteRecursively()
    }

    @Test fun `directories are restored recursively and sized recursively`() = runTest {
        val root = sandbox("bin-directory")
        val bin = root.resolve("bin")
        val manager = RecycleBinManager(bin)
        val folder = root.resolve("Series/Season 1").apply { mkdirs() }
        folder.resolve("e01.mkv").writeText("12345")
        folder.resolve("e02.mkv").writeText("123")
        manager.delete(listOf(folder)).getOrThrow()
        val entry = manager.entries().single()
        assertEquals(8L, entry.size)
        val restored = manager.restore(RecycleEntryRecord(entry.originalPath, entry.deletedPath)).getOrThrow()
        assertTrue(restored.isDirectory)
        assertEquals("12345", restored.resolve("e01.mkv").readText())
        assertEquals("123", restored.resolve("e02.mkv").readText())
        root.deleteRecursively()
    }

    @Test fun `restore falls back to copy and delete when rename is impossible`() = runTest {
        val otherFileSystem = java.io.File("/dev/shm")
        if (!otherFileSystem.isDirectory || !otherFileSystem.canWrite()) return@runTest // environment dependent
        val root = sandbox("bin-cross-fs")
        val bin = root.resolve("bin")
        val manager = RecycleBinManager(bin)
        val original = Files.createTempDirectory(otherFileSystem.toPath(), "origin").toFile().resolve("clip.mp4")
        original.writeText("cross")
        manager.delete(listOf(original)).getOrThrow()
        val entry = manager.entries().single()
        val restored = manager.restore(RecycleEntryRecord(entry.originalPath, entry.deletedPath)).getOrThrow()
        assertEquals("cross", restored.readText())
        assertFalse(java.io.File(entry.deletedPath).exists())
        assertFalse(java.io.File(entry.deletedPath + RecycleBinManager.METADATA_SUFFIX).exists())
        root.deleteRecursively()
        original.parentFile.deleteRecursively()
    }

    @Test fun `emptying the bin removes stored items and metadata`() = runTest {
        val root = sandbox("bin-empty")
        val bin = root.resolve("bin")
        val manager = RecycleBinManager(bin)
        val file = root.resolve("clip.mp4").apply { writeText("x") }
        manager.delete(listOf(file)).getOrThrow()
        assertEquals(1, manager.entries().size)
        manager.empty().getOrThrow()
        assertTrue(manager.entries().isEmpty())
        root.deleteRecursively()
    }
}
