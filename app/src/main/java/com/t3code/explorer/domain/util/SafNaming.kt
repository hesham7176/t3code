package com.t3code.explorer.domain.util

/**
 * Naming rules for Storage Access Framework documents.
 *
 * SAF names are display names inside a document provider, not paths: they are validated with the
 * same rules as local names and de-duplicated through [FileOperationPolicy.conflictName] so the
 * provider never silently replaces an existing document.
 */
object SafNaming {
    fun safeName(value: String): String = FileOperationPolicy.safeName(value)

    /** Directory aware de-duplication; [exists] asks the document provider whether a name is taken. */
    fun conflictName(requested: String, isDirectory: Boolean, exists: (String) -> Boolean): String =
        FileOperationPolicy.conflictName(safeName(requested), isDirectory, exists)
}
