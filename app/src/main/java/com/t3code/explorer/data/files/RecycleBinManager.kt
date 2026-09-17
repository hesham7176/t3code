package com.t3code.explorer.data.files

import com.t3code.explorer.domain.model.OperationProgress
import com.t3code.explorer.domain.model.RecycleEntry
import com.t3code.explorer.domain.util.FileOperationPolicy
import com.t3code.explorer.domain.util.RecycleMetadataCodec
import com.t3code.explorer.domain.util.runCatchingCancellable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class RecycleEntryRecord(val originalPath: String, val deletedPath: String)

/**
 * Recycle bin storage.
 *
 * Deleting moves the item inside [binDirectory] and stores the original absolute path next to it
 * (`.meta` sidecar). Restoring puts the item back at its original location; because the bin lives
 * on internal storage and the original usually lives on shared storage, rename is not sufficient,
 * so a copy + delete fallback is used and the returned metadata is only removed after a verified
 * restore.
 */
class RecycleBinManager(
    private val binDirectory: File,
    private val onProgress: (OperationProgress) -> Unit = {}
) {
    suspend fun delete(items: List<File>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingCancellable {
            require(binDirectory.isDirectory || binDirectory.mkdirs()) { "Recycle bin is unavailable" }
            items.forEachIndexed { index, source ->
                coroutineContext.ensureActive()
                require(source.exists()) { "Source file is missing: ${source.path}" }
                val target = FileOperationPolicy.conflictSafe(File(binDirectory, "${System.currentTimeMillis()}_${UUID.randomUUID()}_${source.name}"))
                onProgress(OperationProgress("delete", source.name, index.toLong(), items.size.toLong(), index, items.size))
                val moved = source.renameTo(target) || copyThenDelete(source, target)
                require(moved) { "Delete failed for ${source.name}" }
                val metadata = File(target.parentFile, "${target.name}.meta")
                metadata.writeText(RecycleMetadataCodec.encode(source.absolutePath))
            }
            onProgress(OperationProgress("delete", "", items.size.toLong(), items.size.toLong(), items.size, items.size, isComplete = true))
        }
    }

    /**
     * Restores [record] to its original path.
     *
     * - Rejects blank or relative metadata instead of writing to an unexpected location.
     * - Uses a conflict-safe name, so an item that now exists at the original path is not replaced.
     * - Falls back to copy + delete when the bin and the original path are on different filesystems.
     * - Only removes the metadata sidecar after the item was verified at its destination.
     */
    suspend fun restore(record: RecycleEntryRecord): Result<File> = withContext(Dispatchers.IO) {
        runCatchingCancellable {
            val original = record.originalPath
            require(original.isNotBlank()) { "Original path metadata is missing" }
            require(File(original).isAbsolute) { "Original path metadata is invalid" }
            val deleted = File(record.deletedPath)
            require(deleted.exists()) { "The deleted item is no longer in the recycle bin" }
            val requested = File(original)
            val parent = requested.parentFile
            require(parent != null) { "Original path metadata is invalid" }
            require(parent.isDirectory || parent.mkdirs()) { "Could not recreate the original folder" }
            val target = FileOperationPolicy.conflictSafe(requested)
            coroutineContext.ensureActive()
            require(deleted.renameTo(target) || copyThenDelete(deleted, target)) { "Restore failed" }
            require(target.exists()) { "Restore failed: the item is not at its original location" }
            File("${record.deletedPath}.meta").delete()
            target
        }
    }

    /** Lists bin entries; entries whose metadata is missing or unreadable keep an empty original path. */
    fun entries(): List<RecycleEntry> {
        if (!binDirectory.isDirectory) return emptyList()
        return binDirectory.listFiles().orEmpty()
            .asSequence()
            .filter { !it.name.endsWith(METADATA_SUFFIX) }
            .map { item ->
                val metadata = File(item.parentFile, item.name + METADATA_SUFFIX)
                val original = decodeOriginalPath(metadata)
                RecycleEntry(
                    id = item.name,
                    originalPath = original,
                    deletedPath = item.absolutePath,
                    deletedAt = item.lastModified(),
                    size = LocalFileOperations.recursiveSize(item),
                    name = original.takeIf { it.isNotBlank() }?.let(::File)?.name ?: item.name.substringAfter('_').substringAfter('_')
                )
            }
            .sortedByDescending { it.deletedAt }
            .toList()
    }

    fun empty(): Result<Unit> = runCatching {
        binDirectory.deleteRecursively()
        Unit
    }

    /** Recursive copy followed by recursive deletion of the source; used across filesystem boundaries. */
    private suspend fun copyThenDelete(source: File, target: File): Boolean {
        val copied = runCatchingCancellable {
            copyRecursive(source, target)
            true
        }.getOrDefault(false)
        if (!copied) {
            LocalFileOperations.deleteRecursively(target)
            return false
        }
        return LocalFileOperations.deleteRecursively(source)
    }

    private suspend fun copyRecursive(source: File, target: File) {
        coroutineContext.ensureActive()
        if (source.isDirectory) {
            require(target.mkdirs() || target.isDirectory) { "Could not create ${target.path}" }
            source.listFiles()?.forEach { child -> copyRecursive(child, File(target, child.name)) }
        } else {
            target.parentFile?.mkdirs()
            FileInputStream(source).use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read: Int
                    while (input.read(buffer).also { read = it } >= 0) {
                        coroutineContext.ensureActive()
                        if (read > 0) output.write(buffer, 0, read)
                    }
                }
            }
        }
    }

    private fun decodeOriginalPath(metadata: File): String {
        if (!metadata.exists()) return ""
        val encoded = runCatching { metadata.readText() }.getOrDefault("")
        return RecycleMetadataCodec.decode(encoded)
    }

    companion object {
        const val METADATA_SUFFIX = ".meta"
    }
}
