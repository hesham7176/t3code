package com.t3code.explorer

import com.t3code.explorer.domain.util.ArchiveManager
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    @Test fun `existing archive and extracted file use conflict-safe names`() = runTest {
        val root = Files.createTempDirectory("archive-conflicts").toFile()
        val source = root.resolve("source").apply { mkdirs() }
        source.resolve("note.txt").writeText("new")
        val archive = root.resolve("archive.zip")
        ArchiveManager().createZip(source, archive)
        ArchiveManager().createZip(source, archive)
        assertTrue(root.resolve("archive (1).zip").isFile)
        val destination = root.resolve("out").apply { mkdirs() }
        destination.resolve("source").mkdirs()
        destination.resolve("source/note.txt").writeText("old")
        ArchiveManager().extractZip(archive, destination)
        assertEquals("old", destination.resolve("source/note.txt").readText())
        assertEquals("new", destination.resolve("source/note (1).txt").readText())
        root.deleteRecursively()
    }

    @Test fun `zip slip entry is rejected`() = runTest {
        val root = Files.createTempDirectory("zip-slip").toFile()
        val zip = root.resolve("unsafe.zip")
        ZipOutputStream(zip.outputStream()).use { output ->
            output.putNextEntry(ZipEntry("../outside.txt"))
            output.write("blocked".toByteArray())
            output.closeEntry()
        }
        assertFailsWith<IllegalArgumentException> { ArchiveManager().extractZip(zip, root.resolve("out")) }
        assertTrue(!root.parentFile.resolve("outside.txt").exists())
        root.deleteRecursively()
    }
}
