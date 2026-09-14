package com.t3code.explorer.data.files

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * Small SAF adapter. It deliberately exposes only operations that can be
 * completed through DocumentFile without pretending that a tree URI is a
 * normal java.io.File.
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

    private fun requireDirectory(uri: Uri): DocumentFile =
        document(uri)?.also { require(it.isDirectory) { "SAF location is not a directory" } }
            ?: error("SAF location is unavailable")

    private fun document(uri: Uri): DocumentFile? =
        DocumentFile.fromTreeUri(context, uri) ?: DocumentFile.fromSingleUri(context, uri)

    private fun safeName(value: String): String =
        value.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "untitled" }
}
