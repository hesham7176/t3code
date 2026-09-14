package com.t3code.explorer.domain.util

import com.t3code.explorer.domain.model.FileItem
import com.t3code.explorer.domain.model.SortDirection
import com.t3code.explorer.domain.model.SortField
import com.t3code.explorer.domain.model.SortSpec

fun List<FileItem>.sortedBySpec(spec: SortSpec): List<FileItem> {
    val comparator = compareBy<FileItem> { !it.isDirectory }.thenComparator { left, right ->
        when (spec.field) {
            SortField.NAME -> left.name.lowercase().compareTo(right.name.lowercase())
            SortField.TYPE -> left.extension.compareTo(right.extension)
            SortField.SIZE -> left.size.compareTo(right.size)
            SortField.MODIFIED -> left.modifiedAt.compareTo(right.modifiedAt)
        }
    }
    return if (spec.direction == SortDirection.ASCENDING) sortedWith(comparator) else sortedWith(comparator.reversed())
}
