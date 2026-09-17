package com.t3code.explorer.data.files

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.t3code.explorer.domain.util.SafNaming
import com.t3code.explorer.domain.util.runCatchingCancellable
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * SAF adapter; tree/document URIs never pass through java.io.File.
 *
 * Every operation works on [DocumentFile] / `content://` URIs and streams bytes through the
 * ContentResolver, so removable storage, USB OTG and document providers are supported without
 * assuming a mountable path.
 */
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
        val safeName = SafNaming.safeName(name)
        require(parent.findFile(safeName) == null) { "An item with this name already exists" }
        parent.createDirectory(safeName)?.uri ?: error("Could not create directory")
    }

    fun createFile(parentUri: Uri, mimeType: String, name: String): Result<Uri> = runCatching {
        val parent = requireDirectory(parentUri)
        val safeName = SafNaming.safeName(name)
        require(parent.findFile(safeName) == null) { "An item with this name already exists" }
        parent.createFile(mimeType.ifBlank { "application/octet-stream" }, safeName)?.uri
            ?: error("Could not create file")
    }

    fun rename(itemUri: Uri, name: String): Result<Uri> = runCatching {
        val item = document(itemUri) ?: error("SAF item is unavailable")
        val safeName = SafNaming.safeName(name)
        val parent = item.parentFile
        require(item.name != safeName) { "The name is unchanged" }
        if (parent != null) require(parent.findFile(safeName) == null) { "An item with this name already exists" }
        require(item.renameTo(safeName)) { "Rename failed" }
        item.uri
    }

    suspend fun copy(items: List<Uri>, destinationUri: Uri, onBytes: (Long) -> Unit = {}): Result<Unit> =
        transfer(items, destinationUri, deleteSources = false, onBytes)

    suspend fun move(items: List<Uri>, destinationUri: Uri, onBytes: (Long) -> Unit = {}): Result<Unit> =
        transfer(items, destinationUri, deleteSources = true, onBytes)

    /**
     * Permanent deletion of SAF documents.
     *
     * SAF has no system trash, so this cannot be undone: the UI asks for confirmation before
     * calling it. Children are deleted before their parent so providers that refuse to delete a
     * non-empty directory still end up empty.
     */
    suspend fun delete(items: List<Uri>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingCancellable {
            items.forEach { uri ->
                coroutineContext.ensureActive()
                val item = document(uri) ?: error("SAF item is unavailable")
                require(deleteRecursive(item)) { "Could not delete ${item.name.orEmpty()}" }
            }
        }
    }

    private suspend fun deleteRecursive(item: DocumentFile): Boolean {
        coroutineContext.ensureActive()
        if (item.isDirectory) {
            item.listFiles().forEach { child -> if (!deleteRecursive(child)) return false }
        }
        return item.delete()
    }

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
        val isDirectory = source.isDirectory
        val name = SafNaming.conflictName(source.name.orEmpty(), isDirectory) { candidate -> destination.findFile(candidate) != null }
        if (isDirectory) {
            val target = destination.createDirectory(name) ?: error("Could not create SAF directory")
            created += target
            source.listFiles().forEach { child -> copyDocument(child, target, created, onBytes) }
            return target
        }
        val target = destination.createFile(source.type ?: "application/octet-stream", name) ?: error("Could not create SAF file")
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
            if (read > 0) {
                output.write(buffer, 0, read)
                onBytes(read.toLong())
            }
        }
    }

    private fun requireDirectory(uri: Uri): DocumentFile =
        document(uri)?.also { require(it.isDirectory) { "SAF location is not a directory" } }
            ?: error("SAF location is unavailable")

    private fun document(uri: Uri): DocumentFile? =
        DocumentFile.fromTreeUri(context, uri) ?: DocumentFile.fromSingleUri(context, uri)
}
