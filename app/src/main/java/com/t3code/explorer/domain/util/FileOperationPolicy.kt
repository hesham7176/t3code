package com.t3code.explorer.domain.util

import java.io.File

/** Shared safety rules for local operations and archive extraction. */
object FileOperationPolicy {
    fun safeName(value: String): String {
        val cleaned = value.trim().replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
        return cleaned.takeUnless { it.isBlank() || it == "." || it == ".." } ?: "untitled"
    }

    fun conflictSafe(file: File): File {
        if (!file.exists()) return file
        val extension = file.extension.takeIf { it.isNotBlank() }?.let { ".${it}" }.orEmpty()
        val stem = file.name.removeSuffix(extension)
        return generateSequence(1) { it + 1 }
            .map { File(file.parentFile, "$stem ($it)$extension") }
            .first { !it.exists() }
    }

    fun isInsideOrEqual(parent: File, child: File): Boolean {
        val parentPath = parent.canonicalPath
        val childPath = child.canonicalPath
        return childPath == parentPath || childPath.startsWith(parentPath + File.separator)
    }
}
