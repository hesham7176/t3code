package com.t3code.explorer.data.files

import android.content.Context
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.t3code.explorer.domain.model.FileCategory
import com.t3code.explorer.domain.model.FileItem
import com.t3code.explorer.domain.model.SearchFilters
import com.t3code.explorer.domain.model.SortSpec
import com.t3code.explorer.domain.util.FileType
import com.t3code.explorer.domain.util.FolderCoverResolver
import com.t3code.explorer.domain.util.runCatchingCancellable
import com.t3code.explorer.domain.util.sortedBySpec
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class FileRepository(
    private val context: Context,
    private val coverResolver: FolderCoverResolver = FolderCoverResolver()
) {
    suspend fun list(path: String, sort: SortSpec = SortSpec(), showHidden: Boolean = false): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        runCatchingCancellable {
            val directory = File(path)
            require(directory.isDirectory) { "Not a directory: $path" }
            val items = directory.listFiles()?.asSequence()
                .orEmpty()
                .filter { showHidden || !it.isHidden }
                .map(::toItem)
                ?.toList()
                ?: emptyList()
            items.sortedBySpec(sort)
        }
    }

    suspend fun search(root: String, filters: SearchFilters, showHidden: Boolean = false): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        runCatchingCancellable {
            val start = File(root)
            if (!start.exists()) return@runCatchingCancellable emptyList()
            val found = ArrayList<FileItem>()
            start.walkTopDown()
                .onEnter { directory -> !directory.isHidden || showHidden }
                .takeWhile { found.size < 10_000 }
                .forEach { file ->
                    coroutineContext.ensureActive()
                    if (!showHidden && file.isHidden) return@forEach
                    val item = toItem(file)
                    if (matches(item, filters)) found += item
                }
            found.sortedBy { it.name.lowercase(Locale.ROOT) }
        }
    }

    suspend fun listSafTree(treeUri: android.net.Uri, sort: SortSpec = SortSpec(), showHidden: Boolean = false): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        runCatchingCancellable {
            val root = document(treeUri) ?: error("Invalid storage URI")
            require(root.isDirectory) { "Selected SAF item is not a directory" }
            root.listFiles().asSequence()
                .filter { showHidden || !it.name.orEmpty().startsWith('.') }
                .map(::toSafItem)
                .toList()
                .sortedBySpec(sort)
        }
    }

    suspend fun searchSafTree(treeUri: android.net.Uri, filters: SearchFilters, showHidden: Boolean = false): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        runCatchingCancellable {
            val root = document(treeUri) ?: error("Invalid storage URI")
            require(root.isDirectory) { "Selected SAF item is not a directory" }
            val found = ArrayList<FileItem>()
            suspend fun visit(directory: DocumentFile) {
                coroutineContext.ensureActive()
                directory.listFiles().forEach { child ->
                    coroutineContext.ensureActive()
                    if (!showHidden && child.name.orEmpty().startsWith('.')) return@forEach
                    val item = toSafItem(child)
                    if (matches(item, filters)) found += item
                    if (child.isDirectory && found.size < 10_000) visit(child)
                }
            }
            visit(root)
            found.sortedBy { it.name.lowercase(Locale.ROOT) }
        }
    }

    fun toItem(file: File): FileItem {
        val cover = if (file.isDirectory) coverResolver.resolve(file)?.absolutePath else null
        val type = if (file.isDirectory) "inode/directory" else FileType.mimeType(file.name)
        return FileItem(
            name = file.name,
            path = file.absolutePath,
            isDirectory = file.isDirectory,
            size = if (file.isDirectory) directorySizeHint(file) else file.length(),
            modifiedAt = file.lastModified(),
            mimeType = type,
            extension = FileType.extension(file.name),
            isHidden = file.isHidden,
            childCount = if (file.isDirectory) file.list()?.size else null,
            coverPath = cover
        )
    }

    private fun toSafItem(document: DocumentFile): FileItem {
        val name = document.name.orEmpty()
        val mime = document.type ?: if (document.isDirectory) "inode/directory" else FileType.mimeType(name)
        return FileItem(name, document.uri.toString(), document.uri, document.isDirectory, document.length().coerceAtLeast(0), document.lastModified(), mime, FileType.extension(name), name.startsWith('.'))
    }

    private fun matches(item: FileItem, filters: SearchFilters): Boolean {
        val queryMatches = filters.query.isBlank() || item.name.contains(filters.query, ignoreCase = true)
        val categoryMatches = filters.category == null || FileType.category(item.name, item.mimeType) == filters.category
        val extMatches = filters.extension.isNullOrBlank() || FileType.extension(item.name) == filters.extension!!.trimStart('.').lowercase(Locale.ROOT)
        val minMatches = filters.minimumSize == null || item.size >= filters.minimumSize
        val maxMatches = filters.maximumSize == null || item.size <= filters.maximumSize
        return queryMatches && categoryMatches && extMatches && minMatches && maxMatches
    }

    private fun document(uri: android.net.Uri): DocumentFile? =
        DocumentFile.fromTreeUri(context, uri) ?: DocumentFile.fromSingleUri(context, uri)

    private fun directorySizeHint(file: File): Long = 0L
    fun primaryPath(): String = Environment.getExternalStorageDirectory().absolutePath
}
