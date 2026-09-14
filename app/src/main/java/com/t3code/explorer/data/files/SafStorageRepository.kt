package com.t3code.explorer.data.files

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

class SafStorageRepository(private val context: Context) {
    fun takePersistablePermission(uri: Uri, flags: Int) {
        context.contentResolver.takePersistableUriPermission(uri, flags and (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
    }

    fun persistedTrees(): List<Uri> = context.contentResolver.persistedUriPermissions.map { it.uri }

    fun root(uri: Uri): DocumentFile? = DocumentFile.fromTreeUri(context, uri)
}
