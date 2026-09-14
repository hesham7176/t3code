package com.t3code.explorer.domain.util

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Resolves folder art without recursively walking the storage. Results are cached by directory timestamp. */
class FolderCoverResolver {
    private val names = listOf("cover", "poster", "folder")
    private val extensions = setOf("jpg", "jpeg", "png", "webp")
    private val cache = ConcurrentHashMap<String, CachedCover>()

    fun resolve(directory: File): File? {
        if (!directory.isDirectory) return null
        val key = directory.absolutePath
        val stamp = directory.lastModified()
        cache[key]?.takeIf { it.directoryStamp == stamp }?.let { return it.path?.let(::File) }
        val result = find(directory)
        cache[key] = CachedCover(stamp, result?.absolutePath)
        return result
    }

    fun invalidate(directory: File) { cache.remove(directory.absolutePath) }
    fun clear() { cache.clear() }

    private fun find(directory: File): File? {
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

    private data class CachedCover(val directoryStamp: Long, val path: String?)
}
