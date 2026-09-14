package com.t3code.explorer.data.files

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.t3code.explorer.domain.model.FileItem
import com.t3code.explorer.domain.model.OperationProgress
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class FileOperationManager(private val context: Context) {
    private val _progress = MutableStateFlow<OperationProgress?>(null)
    val progress: StateFlow<OperationProgress?> = _progress.asStateFlow()

    suspend fun copy(items: List<FileItem>, destination: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(destination.isDirectory || destination.mkdirs()) { "Destination is unavailable" }
            val files = items.map { File(it.path) }
            files.forEach { source ->
                require(source.exists()) { "Source file is missing: ${source.path}" }
                val sourcePath = source.canonicalPath
                val destinationPath = destination.canonicalPath
                require(destinationPath != sourcePath && !destinationPath.startsWith(sourcePath + File.separator)) { "Cannot copy a folder into itself" }
            }
            val totalBytes = files.sumOf(::recursiveSize)
            var completedBytes = 0L
            files.forEachIndexed { index, source ->
                val target = conflictSafe(File(destination, source.name))
                copyRecursive(source, target) { delta ->
                    completedBytes += delta
                    _progress.value = OperationProgress("copy", source.name, completedBytes, totalBytes, index, files.size)
                }
            }
            _progress.value = OperationProgress("copy", "", totalBytes, totalBytes, files.size, files.size, isComplete = true)
        }.also { if (it.isFailure) _progress.value = _progress.value?.copy(error = it.exceptionOrNull()?.message) }
    }

    suspend fun move(items: List<FileItem>, destination: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(destination.isDirectory || destination.mkdirs())
            val sources = items.map { File(it.path) }
            sources.forEach { require(it.exists()) { "Source file is missing: ${it.path}" } }
            val totalBytes = sources.sumOf(::recursiveSize)
            var completedBytes = 0L
            sources.forEachIndexed { index, source ->
                coroutineContext.ensureActive()
                val destinationPath = destination.canonicalPath
                require(destinationPath != source.canonicalPath && !destinationPath.startsWith(source.canonicalPath + File.separator)) { "Cannot move a folder into itself" }
                val target = conflictSafe(File(destination, source.name))
                if (!source.renameTo(target)) {
                    copyRecursive(source, target) { delta ->
                        completedBytes += delta
                        _progress.value = OperationProgress("move", source.name, completedBytes, totalBytes, index, sources.size)
                    }
                    require(deleteRecursively(source)) { "Could not remove the original after copying" }
                } else {
                    completedBytes += recursiveSize(target)
                    _progress.value = OperationProgress("move", source.name, completedBytes, totalBytes, index + 1, sources.size)
                }
            }
            _progress.value = OperationProgress("move", "", totalBytes, totalBytes, sources.size, sources.size, isComplete = true)
        }.also { if (it.isFailure) _progress.value = _progress.value?.copy(error = it.exceptionOrNull()?.message) }
    }

    suspend fun createFolder(parent: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching { File(parent, safeName(name)).also { require(it.mkdirs()) { "Could not create folder" } } }
    }

    suspend fun createFile(parent: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching { File(parent, safeName(name)).also { require(it.createNewFile()) { "File already exists" } } }
    }

    suspend fun rename(item: FileItem, name: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val source = File(item.path)
            val target = File(source.parentFile ?: error("No parent"), safeName(name))
            require(!target.exists()) { "A file with this name already exists" }
            require(source.renameTo(target)) { "Rename failed" }
            target
        }
    }

    suspend fun deleteToRecycleBin(items: List<FileItem>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bin = File(context.filesDir, "recycle-bin").apply { mkdirs() }
            items.forEach { item ->
                val source = File(item.path)
                require(source.exists()) { "Source file is missing: ${item.path}" }
                val target = conflictSafe(File(bin, "${System.currentTimeMillis()}_${UUID.randomUUID()}_${source.name}"))
                val metadata = File(target.parentFile, "${target.name}.meta")
                require(source.renameTo(target) || copyAndDelete(source, target)) { "Delete failed" }
                metadata.writeText(java.util.Base64.getEncoder().encodeToString(item.path.toByteArray(Charsets.UTF_8)))
            }
        }
    }

    suspend fun restoreRecycleEntry(entry: RecycleEntryRecord): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val target = conflictSafe(File(entry.originalPath))
            target.parentFile?.mkdirs()
            require(File(entry.deletedPath).renameTo(target)) { "Restore failed" }
            File(entry.deletedPath + ".meta").delete()
            Unit
        }
    }

    fun shareIntent(item: FileItem): Intent? {
        val file = File(item.path)
        val uri = item.uri ?: FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        if (item.uri == null && !file.exists()) return null
        return Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun safeName(value: String): String = value.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "untitled" }

    private fun conflictSafe(file: File): File {
        if (!file.exists()) return file
        val ext = file.extension.takeIf { it.isNotBlank() }?.let { ".${it}" }.orEmpty()
        val stem = file.name.removeSuffix(ext)
        return generateSequence(1) { it + 1 }.map { File(file.parentFile, "$stem ($it)$ext") }.first { !it.exists() }
    }

    private suspend fun copyRecursive(source: File, target: File, onBytes: (Long) -> Unit) {
        coroutineContext.ensureActive()
        if (source.isDirectory) {
            require(target.mkdirs() || target.isDirectory) { "Could not create ${target.path}" }
            source.listFiles()?.forEach { child -> copyRecursive(child, File(target, child.name), onBytes) }
        } else {
            target.parentFile?.mkdirs()
            FileInputStream(source).use { input -> FileOutputStream(target).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var read: Int
                while (input.read(buffer).also { read = it } >= 0) {
                    coroutineContext.ensureActive()
                    if (read > 0) { output.write(buffer, 0, read); onBytes(read.toLong()) }
                }
            }}
        }
    }

    private fun copyAndDelete(source: File, target: File): Boolean = runCatching {
        source.copyRecursively(target, overwrite = false)
        source.deleteRecursively()
        true
    }.getOrDefault(false)

    private fun deleteRecursively(file: File): Boolean = file.deleteRecursively()
    private fun recursiveSize(file: File): Long = if (file.isFile) file.length() else file.listFiles()?.sumOf(::recursiveSize) ?: 0L
}

data class RecycleEntryRecord(val originalPath: String, val deletedPath: String)
