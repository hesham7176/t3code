package com.t3code.explorer.data.text

import android.content.Context
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import com.t3code.explorer.domain.util.runCatchingCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TextDocumentRepository(private val context: Context? = null) {
    suspend fun read(file: File, maxBytes: Long = DEFAULT_MAX_BYTES): Result<String> = withContext(Dispatchers.IO) {
        runCatchingCancellable {
            require(file.isFile) { "File is unavailable" }
            decodeUtf8(readLimited(file.inputStream(), maxBytes), maxBytes)
        }
    }

    suspend fun read(uri: Uri, maxBytes: Long = DEFAULT_MAX_BYTES): Result<String> = withContext(Dispatchers.IO) {
        runCatchingCancellable {
            val resolver = requireNotNull(context) { "A Context is required for SAF documents" }
            val input = resolver.contentResolver.openInputStream(uri) ?: error("SAF document is unavailable")
            input.use { decodeUtf8(readLimited(it, maxBytes), maxBytes) }
        }
    }

    suspend fun write(file: File, content: String, maxBytes: Long = DEFAULT_MAX_BYTES): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingCancellable {
            val bytes = content.toByteArray(StandardCharsets.UTF_8)
            require(bytes.size <= maxBytes) { "Text is too large for the editor" }
            file.parentFile?.mkdirs()
            val temporary = File(file.parentFile ?: error("File has no parent"), ".${file.name}.editing")
            temporary.writeBytes(bytes)
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: Exception) {
                try {
                    Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                } catch (_: Exception) {
                    require(temporary.renameTo(file)) { "Could not save file" }
                }
            } finally {
                temporary.delete()
            }
        }
    }

    suspend fun write(uri: Uri, content: String, maxBytes: Long = DEFAULT_MAX_BYTES): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingCancellable {
            val resolver = requireNotNull(context) { "A Context is required for SAF documents" }
            val bytes = content.toByteArray(StandardCharsets.UTF_8)
            require(bytes.size <= maxBytes) { "Text is too large for the editor" }
            val output = resolver.contentResolver.openOutputStream(uri, "wt") ?: error("SAF document is unavailable")
            output.use { it.write(bytes) }
        }
    }

    fun isEditable(file: File) = isEditable(file.extension)
    fun isEditable(uri: Uri, name: String? = uri.lastPathSegment) = isEditable(name.orEmpty().substringAfterLast('.'))

    private fun isEditable(extension: String) = extension.lowercase() in EDITABLE_EXTENSIONS

    private fun decodeUtf8(bytes: ByteArray, maxBytes: Long): String {
        require(bytes.size <= maxBytes) { "File is too large for the editor" }
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return decoder.decode(ByteBuffer.wrap(bytes)).toString()
    }

    private fun readLimited(input: java.io.InputStream, maxBytes: Long): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        var read: Int
        while (input.read(buffer).also { read = it } >= 0) {
            if (read == 0) continue
            total += read
            require(total <= maxBytes) { "File is too large for the editor" }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    companion object {
        const val DEFAULT_MAX_BYTES = 5L * 1024 * 1024
        private val EDITABLE_EXTENSIONS = setOf("txt", "json", "xml", "html", "css", "js", "kt", "java", "md", "csv")
    }
}
