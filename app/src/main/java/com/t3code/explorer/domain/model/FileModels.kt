package com.t3code.explorer.domain.model

import android.net.Uri

/** A single filesystem or SAF item displayed by the browser. */
data class FileItem(
    val name: String,
    val path: String,
    val uri: Uri? = null,
    val isDirectory: Boolean,
    val size: Long,
    val modifiedAt: Long,
    val mimeType: String,
    val extension: String,
    val isHidden: Boolean = false,
    val childCount: Int? = null,
    val coverPath: String? = null
)

data class StorageLocation(
    val id: String,
    val name: String,
    val path: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val isRemovable: Boolean = false,
    val treeUri: Uri? = null
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0)
    val usagePercent: Int
        get() = if (totalBytes <= 0) 0 else ((usedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
}

enum class FileCategory(val key: String) {
    IMAGE("image"), VIDEO("video"), AUDIO("audio"), DOCUMENT("document"), ARCHIVE("archive"), APK("apk"), OTHER("other")
}

enum class SortField { NAME, TYPE, SIZE, MODIFIED }

enum class SortDirection { ASCENDING, DESCENDING }

data class SortSpec(val field: SortField = SortField.NAME, val direction: SortDirection = SortDirection.ASCENDING)

enum class ViewMode(val isGrid: Boolean, val detailLines: Int) {
    SMALL_GRID(true, 0), MEDIUM_GRID(true, 0), LARGE_GRID(true, 0),
    SMALL_LIST(false, 0), MEDIUM_LIST(false, 0), LARGE_LIST(false, 0),
    SMALL_DETAILS(false, 2), MEDIUM_DETAILS(false, 2), LARGE_DETAILS(false, 2)
}

data class OperationProgress(
    val operation: String,
    val currentName: String,
    val completedBytes: Long,
    val totalBytes: Long,
    val completedItems: Int,
    val totalItems: Int,
    val isComplete: Boolean = false,
    val error: String? = null
) {
    val percent: Int
        get() = when {
            totalBytes > 0 -> ((completedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
            isComplete -> 100
            else -> 0
        }
}

data class SearchFilters(
    val query: String,
    val category: FileCategory? = null,
    val extension: String? = null,
    val minimumSize: Long? = null,
    val maximumSize: Long? = null
)

data class StorageAnalysis(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val categoryBytes: Map<FileCategory, Long>,
    val largestFiles: List<FileItem>,
    val largestDirectories: List<FileItem>
)

data class RecycleEntry(
    val id: String,
    val originalPath: String,
    val deletedPath: String,
    val deletedAt: Long,
    val size: Long,
    val name: String
)
