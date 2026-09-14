package com.t3code.explorer

import com.t3code.explorer.domain.util.ArchiveManager
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

class ArchiveManagerTest {
    @Test fun `zip round trip keeps files inside destination`() = runTest {
        val root = Files.createTempDirectory("archive").toFile()
        val source = root.resolve("source").apply { mkdirs() }
        source.resolve("hello.txt").writeText("hello")
        val zip = root.resolve("archive.zip")
        val destination = root.resolve("out")
        ArchiveManager().createZip(source, zip)
        ArchiveManager().extractZip(zip, destination)
        assertTrue(destination.walkTopDown().any { it.name == "hello.txt" })
        root.deleteRecursively()
    }
}
