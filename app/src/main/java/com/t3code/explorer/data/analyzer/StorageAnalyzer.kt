package com.t3code.explorer.data.analyzer

import com.t3code.explorer.domain.model.FileCategory
import com.t3code.explorer.domain.model.FileItem
import com.t3code.explorer.domain.model.StorageAnalysis
import com.t3code.explorer.domain.util.FileType
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class StorageAnalyzer {
    suspend fun analyze(root: File): Result<StorageAnalysis> = withContext(Dispatchers.IO) {
        runCatching {
            val categories = FileCategory.values().associateWith { 0L }.toMutableMap()
            val files = mutableListOf<FileItem>()
            val directorySizes = mutableMapOf<File, Long>()
            root.walkTopDown().forEach { file ->
                coroutineContext.ensureActive()
                if (file.isDirectory) {
                    directorySizes.putIfAbsent(file, 0L)
                } else {
                    val size = file.length()
                    val category = FileType.category(file.name)
                    categories[category] = categories.getValue(category) + size
                    if (files.size < 200) files += FileItem(file.name, file.path, isDirectory = false, size = size, modifiedAt = file.lastModified(), mimeType = FileType.mimeType(file.name), extension = FileType.extension(file.name))
                    var parent = file.parentFile
                    while (parent != null && (parent == root || parent.path.startsWith(root.path + File.separator))) {
                        directorySizes[parent] = (directorySizes[parent] ?: 0L) + size
                        if (parent == root) break
                        parent = parent.parentFile
                    }
                }
            }
            val folders = directorySizes.filterKeys { it != root }.map { (directory, size) ->
                FileItem(directory.name, directory.path, isDirectory = true, size = size, modifiedAt = directory.lastModified(), mimeType = "inode/directory", extension = "")
            }
            val total = root.totalSpace
            StorageAnalysis(total, (total - root.usableSpace).coerceAtLeast(0), root.usableSpace, categories, files.sortedByDescending { it.size }.take(20), folders.sortedByDescending { it.size }.take(20))
        }
    }
}
