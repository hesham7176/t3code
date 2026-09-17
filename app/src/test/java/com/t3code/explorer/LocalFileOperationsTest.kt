package com.t3code.explorer

import com.t3code.explorer.data.files.LocalFileOperations
import com.t3code.explorer.domain.model.OperationProgress
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class LocalFileOperationsTest {
    private fun sandbox(name: String): java.io.File = Files.createTempDirectory(name).toFile()

    @Test fun `copy reproduces a nested folder tree and reports byte progress`() = runTest {
        val root = sandbox("copy-tree")
        val source = root.resolve("source").apply { mkdirs() }
        source.resolve("nested/deeper").mkdirs()
        source.resolve("nested/deeper/clip.mp4").writeText("video")
        source.resolve("cover.jpg").writeText("image")
        val destination = root.resolve("destination").apply { mkdirs() }
        val progress = mutableListOf<OperationProgress>()
        val result = LocalFileOperations { progress += it }.copy(listOf(source), destination)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals("video", destination.resolve("source/nested/deeper/clip.mp4").readText())
        assertEquals("image", destination.resolve("source/cover.jpg").readText())
        assertTrue(progress.any { it.isComplete })
        assertEquals(9L, progress.last().totalBytes)
        assertEquals(9L, progress.last().completedBytes)
        root.deleteRecursively()
    }

    @Test fun `copy never overwrites an existing file`() = runTest {
        val root = sandbox("copy-conflict")
        val source = root.resolve("photo.jpg").apply { writeText("new") }
        val destination = root.resolve("destination").apply { mkdirs() }
        destination.resolve("photo.jpg").writeText("old")
        val operations = LocalFileOperations()
        operations.copy(listOf(source), destination).getOrThrow()
        operations.copy(listOf(source), destination).getOrThrow()
        assertEquals("old", destination.resolve("photo.jpg").readText())
        assertEquals("new", destination.resolve("photo (1).jpg").readText())
        assertEquals("new", destination.resolve("photo (2).jpg").readText())
        root.deleteRecursively()
    }

    @Test fun `copy reports a failure for a missing source`() = runTest {
        val root = sandbox("copy-missing")
        val result = LocalFileOperations().copy(listOf(root.resolve("ghost.mp4")), root)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("missing"))
        root.deleteRecursively()
    }

    @Test fun `move relocates the item and removes the source`() = runTest {
        val root = sandbox("move")
        val source = root.resolve("episode.mkv").apply { writeText("data") }
        val destination = root.resolve("destination").apply { mkdirs() }
        LocalFileOperations().move(listOf(source), destination).getOrThrow()
        assertFalse(source.exists())
        assertEquals("data", destination.resolve("episode.mkv").readText())
        root.deleteRecursively()
    }

    @Test fun `moving a folder into itself is rejected`() = runTest {
        val root = sandbox("move-self")
        val folder = root.resolve("movies").apply { mkdirs() }
        val nested = folder.resolve("movies").apply { mkdirs() }
        val result = LocalFileOperations().move(listOf(folder), nested)
        assertTrue(result.isFailure)
        assertTrue(folder.exists())
        root.deleteRecursively()
    }

    @Test fun `rename keeps the sanitised name and refuses collisions`() = runTest {
        val root = sandbox("rename")
        val file = root.resolve("draft.txt").apply { writeText("text") }
        val operations = LocalFileOperations()
        val renamed = operations.rename(file, "final?name.txt").getOrThrow()
        assertEquals("final_name.txt", renamed.name)
        assertEquals("text", renamed.readText())
        val collision = root.resolve("other.txt").apply { writeText("keep") }
        assertFailsWith<IllegalArgumentException> { operations.rename(collision, "final_name.txt").getOrThrow() }
        assertEquals("keep", collision.readText())
        root.deleteRecursively()
    }

    @Test fun `create folder and file reject unsafe names`() = runTest {
        val root = sandbox("create")
        val operations = LocalFileOperations()
        assertEquals("untitled", operations.createFolder(root, "..").getOrThrow().name)
        assertTrue(operations.createFile(root, "note.txt").getOrThrow().isFile)
        assertTrue(operations.createFile(root, "note.txt").isFailure)
        root.deleteRecursively()
    }
}
