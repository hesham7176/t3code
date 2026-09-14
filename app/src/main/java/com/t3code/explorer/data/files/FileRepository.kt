package com.t3code.explorer.data.files

import android.content.Context
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.t3code.explorer.domain.model.FileCategory
import com.t3code.explorer.domain.model.FileItem
import com.t3code.explorer.domain.model.SearchFilters
import com.t3code.explorer.domain.util.FileType
import com.t3code.explorer.domain.util.FolderCoverResolver
import com.t3code.explorer.domain.util.sortedBySpec
import com.t3code.explorer.domain.model.SortSpec
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
        runCatching {
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
        runCatching {
            val start = File(root)
            if (!start.exists()) return@runCatching emptyList()
            val found = ArrayList<FileItem>()
            start.walkTopDown()
                .onEnter { directory -> !directory.isHidden || showHidden }
                .takeWhile { found.size < 10_000 }
                .forEach { file ->
                    coroutineContext.ensureActive()
                    if (!showHidden && file.isHidden) return@forEach
                    val item = toItem(file)
                    val queryMatches = filters.query.isBlank() || file.name.contains(filters.query, ignoreCase = true)
                    val categoryMatches = filters.category == null || FileType.category(file.name) == filters.category
                    val extMatches = filters.extension.isNullOrBlank() || FileType.extension(file.name) == filters.extension!!.trimStart('.').lowercase(Locale.ROOT)
                    val minMatches = filters.minimumSize == null || file.length() >= filters.minimumSize
                    val maxMatches = filters.maximumSize == null || file.length() <= filters.maximumSize
                    if (queryMatches && categoryMatches && extMatches && minMatches && maxMatches) found += item
                }
            found.sortedBy { it.name.lowercase(Locale.ROOT) }
        }
    }

    suspend fun listSafTree(treeUri: android.net.Uri, showHidden: Boolean = false): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: DocumentFile.fromSingleUri(context, treeUri)
                ?: error("Invalid storage URI")
            require(root.isDirectory) { "Selected SAF item is not a directory" }
            root.listFiles().asSequence()
                .filter { showHidden || !it.name.orEmpty().startsWith('.') }
                .map { document ->
                    val name = document.name.orEmpty()
                    FileItem(name, document.uri.toString(), document.uri, document.isDirectory, document.length(), document.lastModified(), document.type ?: FileType.mimeType(name), FileType.extension(name))
                }.sortedBy { it.name.lowercase() }.toList()
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

    private fun directorySizeHint(file: File): Long = 0L

    fun primaryPath(): String = Environment.getExternalStorageDirectory().absolutePath
}
