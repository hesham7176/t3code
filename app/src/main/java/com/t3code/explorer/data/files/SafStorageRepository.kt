package com.t3code.explorer.data.files

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.t3code.explorer.domain.util.runCatchingCancellable
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** SAF adapter; tree/document URIs never pass through java.io.File. */
class SafStorageRepository(private val context: Context) {
    fun takePersistablePermission(uri: Uri, flags: Int) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            flags and (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        )
    }

    fun persistedTrees(): List<Uri> = context.contentResolver.persistedUriPermissions.map { it.uri }
    fun root(uri: Uri): DocumentFile? = document(uri)

    fun createDirectory(parentUri: Uri, name: String): Result<Uri> = runCatching {
        val parent = requireDirectory(parentUri)
        val safeName = safeName(name)
        require(parent.findFile(safeName) == null) { "An item with this name already exists" }
        parent.createDirectory(safeName)?.uri ?: error("Could not create directory")
    }

    fun createFile(parentUri: Uri, mimeType: String, name: String): Result<Uri> = runCatching {
        val parent = requireDirectory(parentUri)
        val safeName = safeName(name)
        require(parent.findFile(safeName) == null) { "An item with this name already exists" }
        parent.createFile(mimeType.ifBlank { "application/octet-stream" }, safeName)?.uri
            ?: error("Could not create file")
    }

    fun rename(itemUri: Uri, name: String): Result<Uri> = runCatching {
        val item = document(itemUri) ?: error("SAF item is unavailable")
        val safeName = safeName(name)
        require(item.name != safeName) { "The name is unchanged" }
        require(item.parentFile?.findFile(safeName) == null) { "An item with this name already exists" }
        require(item.renameTo(safeName)) { "Rename failed" }
        item.uri
    }

    suspend fun copy(items: List<Uri>, destinationUri: Uri, onBytes: (Long) -> Unit = {}): Result<Unit> =
        transfer(items, destinationUri, deleteSources = false, onBytes)

    suspend fun move(items: List<Uri>, destinationUri: Uri, onBytes: (Long) -> Unit = {}): Result<Unit> =
        transfer(items, destinationUri, deleteSources = true, onBytes)

    private suspend fun transfer(items: List<Uri>, destinationUri: Uri, deleteSources: Boolean, onBytes: (Long) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingCancellable {
            val destination = requireDirectory(destinationUri)
            val created = mutableListOf<DocumentFile>()
            try {
                items.forEach { uri ->
                    coroutineContext.ensureActive()
                    val source = document(uri) ?: error("SAF source is unavailable")
                    val target = copyDocument(source, destination, created, onBytes)
                    if (deleteSources) require(source.delete()) { "Could not remove the original SAF item" }
                    check(target.exists()) { "SAF transfer did not complete" }
                }
            } catch (cancelled: CancellationException) {
                created.asReversed().forEach { it.delete() }
                throw cancelled
            }
        }
    }

    private suspend fun copyDocument(source: DocumentFile, destination: DocumentFile, created: MutableList<DocumentFile>, onBytes: (Long) -> Unit): DocumentFile {
        coroutineContext.ensureActive()
        val name = safeName(source.name.orEmpty())
        if (source.isDirectory) {
            val target = destination.createDirectory(conflictName(destination, name)) ?: error("Could not create SAF directory")
            created += target
            source.listFiles().forEach { child -> copyDocument(child, target, created, onBytes) }
            return target
        }
        val target = destination.createFile(source.type ?: "application/octet-stream", conflictName(destination, name)) ?: error("Could not create SAF file")
        created += target
        val input = context.contentResolver.openInputStream(source.uri) ?: error("Could not open SAF source")
        val output = context.contentResolver.openOutputStream(target.uri) ?: error("Could not open SAF destination")
        input.use { sourceStream -> output.use { targetStream -> copyStream(sourceStream, targetStream, onBytes) } }
        return target
    }

    private suspend fun copyStream(input: InputStream, output: OutputStream, onBytes: (Long) -> Unit) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var read: Int
        while (input.read(buffer).also { read = it } >= 0) {
            coroutineContext.ensureActive()
            if (read > 0) { output.write(buffer, 0, read); onBytes(read.toLong()) }
        }
    }

    private fun requireDirectory(uri: Uri): DocumentFile =
        document(uri)?.also { require(it.isDirectory) { "SAF location is not a directory" } }
            ?: error("SAF location is unavailable")

    private fun document(uri: Uri): DocumentFile? =
        DocumentFile.fromTreeUri(context, uri) ?: DocumentFile.fromSingleUri(context, uri)

    private fun conflictName(parent: DocumentFile, requested: String): String {
        if (parent.findFile(requested) == null) return requested
        val extension = requested.substringAfterLast('.', "").takeIf { it.isNotBlank() }?.let { ".${it}" }.orEmpty()
        val stem = requested.removeSuffix(extension)
        return generateSequence(1) { it + 1 }
            .map { "$stem ($it)$extension" }
            .first { parent.findFile(it) == null }
    }

    private fun safeName(value: String): String {
        val cleaned = value.trim().replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
        return cleaned.takeUnless { it.isBlank() || it == "." || it == ".." } ?: "untitled"
    }
}
