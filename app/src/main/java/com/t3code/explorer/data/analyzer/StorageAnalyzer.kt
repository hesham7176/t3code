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
            val folders = mutableListOf<FileItem>()
            root.walkTopDown().forEach { file ->
                coroutineContext.ensureActive()
                if (file.isDirectory) {
                    if (file != root) folders += FileItem(file.name, file.path, isDirectory = true, size = 0, modifiedAt = file.lastModified(), mimeType = "inode/directory", extension = "")
                } else {
                    val size = file.length()
                    val category = FileType.category(file.name)
                    categories[category] = categories.getValue(category) + size
                    if (files.size < 200) files += FileItem(file.name, file.path, isDirectory = false, size = size, modifiedAt = file.lastModified(), mimeType = FileType.mimeType(file.name), extension = FileType.extension(file.name))
                }
            }
            val total = root.totalSpace
            StorageAnalysis(total, (total - root.usableSpace).coerceAtLeast(0), root.usableSpace, categories, files.sortedByDescending { it.size }.take(20), folders.sortedByDescending { it.size }.take(20))
        }
    }
}
