package com.t3code.explorer.domain.util

import java.io.File

/** Resolves folder art without recursively walking the storage. */
class FolderCoverResolver {
    private val names = listOf("cover", "poster", "folder")
    private val extensions = setOf("jpg", "jpeg", "png", "webp")

    fun resolve(directory: File): File? {
        if (!directory.isDirectory) return null
        val children = directory.listFiles() ?: return null
        val byName = children.asSequence()
            .filter { it.isFile && FileType.extension(it.name) in extensions }
            .associateBy { it.name.substringBeforeLast('.').lowercase() }
        names.firstNotNullOfOrNull { byName[it] }?.let { return it }
        return children.asSequence()
            .filter { it.isFile && FileType.extension(it.name) in extensions }
            .sortedBy { it.name.lowercase() }
            .firstOrNull()
    }
}
