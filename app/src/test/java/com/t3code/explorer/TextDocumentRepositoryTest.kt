package com.t3code.explorer

import com.t3code.explorer.data.text.TextDocumentRepository
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TextDocumentRepositoryTest {
    @Test fun `read and write preserve UTF 8`() = runTest {
        val root = Files.createTempDirectory("text").toFile()
        val file = root.resolve("note.txt")
        val repository = TextDocumentRepository()
        repository.write(file, "مرحبا\n世界").getOrThrow()
        assertEquals("مرحبا\n世界", repository.read(file).getOrThrow())
        root.deleteRecursively()
    }

    @Test fun `invalid UTF 8 is rejected`() = runTest {
        val root = Files.createTempDirectory("text-invalid").toFile()
        val file = root.resolve("bad.txt").apply { writeBytes(byteArrayOf(0xC3.toByte(), 0x28)) }
        assertFailsWith<Exception> { TextDocumentRepository().read(file).getOrThrow() }
        root.deleteRecursively()
    }

    @Test fun `write enforces the editor size limit`() = runTest {
        val root = Files.createTempDirectory("text-size").toFile()
        val file = root.resolve("large.txt")
        val result = TextDocumentRepository().write(file, "12345", maxBytes = 4)
        assertEquals(true, result.isFailure)
        root.deleteRecursively()
    }
}
