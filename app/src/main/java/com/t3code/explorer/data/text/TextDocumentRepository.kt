package com.t3code.explorer.data.text

import java.io.File
import java.nio.charset.Charset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TextDocumentRepository {
    suspend fun read(file: File, maxBytes: Long = 5L * 1024 * 1024): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(file.length() <= maxBytes) { "File is too large for the editor" }
            file.readText(Charset.defaultCharset())
        }
    }

    suspend fun write(file: File, content: String): Result<Unit> = withContext(Dispatchers.IO) { runCatching { file.writeText(content, Charsets.UTF_8) } }
    fun isEditable(file: File) = file.extension.lowercase() in setOf("txt", "json", "xml", "html", "css", "js", "kt", "java", "md", "csv")
}
