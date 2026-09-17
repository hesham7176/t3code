package com.t3code.explorer

import com.t3code.explorer.data.analyzer.StorageAnalyzer
import com.t3code.explorer.domain.model.FileCategory
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StorageAnalyzerTest {
    @Test fun `analysis aggregates recursive sizes and categories`() = runTest {
        val root = Files.createTempDirectory("analysis").toFile()
        root.resolve("movies/nested").mkdirs()
        root.resolve("movies/nested/movie.mp4").writeBytes(ByteArray(7))
        root.resolve("cover.jpg").writeBytes(ByteArray(3))
        val progress = mutableListOf<Int>()
        val result = StorageAnalyzer().analyze(root) { progress += it }.getOrThrow()
        assertEquals(7L, result.categoryBytes[FileCategory.VIDEO])
        assertEquals(3L, result.categoryBytes[FileCategory.IMAGE])
        assertEquals(7L, result.largestDirectories.first { it.name == "movies" }.size)
        assertTrue(progress.isNotEmpty())
        root.deleteRecursively()
    }

    @Test fun `analysis rejects a missing storage root`() = runTest {
        val missing = Files.createTempDirectory("analysis-missing").toFile().apply { delete() }
        assertFailsWith<IllegalArgumentException> { StorageAnalyzer().analyze(missing).getOrThrow() }
    }
}
