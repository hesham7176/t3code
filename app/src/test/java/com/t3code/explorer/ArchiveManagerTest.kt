package com.t3code.explorer

import com.t3code.explorer.domain.util.ArchiveManager
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
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
