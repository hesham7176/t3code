package com.t3code.explorer.domain.util

import java.io.File

/** Shared safety rules for local operations, SAF operations and archive extraction. */
object FileOperationPolicy {
    fun safeName(value: String): String {
        val cleaned = value.trim().replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
        return cleaned.takeUnless { it.isBlank() || it == "." || it == ".." } ?: "untitled"
    }

    /** A sibling path that does not exist yet. Directories keep their dots, files keep their extension. */
    fun conflictSafe(file: File): File {
        if (!file.exists()) return file
        val parent = file.parentFile ?: return file
        return File(parent, conflictName(file.name, file.isDirectory) { File(parent, it).exists() })
    }

    /**
     * Builds a non-colliding name using the `name (1)` convention.
     *
     * @param isDirectory when true the last dot does not start an extension, so a folder called
     *                    `Season 1.5` becomes `Season 1.5 (1)` instead of `Season 1 (1).5`.
     *                    A leading dot (`.bashrc`) is never treated as an extension.
     */
    fun conflictName(requested: String, isDirectory: Boolean, exists: (String) -> Boolean): String {
        if (!exists(requested)) return requested
        val dot = requested.lastIndexOf('.')
        val extension = if (isDirectory || dot <= 0) "" else requested.substring(dot)
        val stem = if (extension.isEmpty()) requested else requested.substring(0, dot)
        return generateSequence(1) { it + 1 }
            .map { "$stem ($it)$extension" }
            .first { !exists(it) }
    }

    fun isInsideOrEqual(parent: File, child: File): Boolean {
        val parentPath = parent.canonicalPath
        val childPath = child.canonicalPath
        return childPath == parentPath || childPath.startsWith(parentPath + File.separator)
    }
}
