package com.t3code.explorer.data.files

import com.t3code.explorer.domain.model.OperationProgress
import com.t3code.explorer.domain.util.FileOperationPolicy
import com.t3code.explorer.domain.util.runCatchingCancellable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Local, `java.io` based file operations.
 *
 * The class intentionally depends on nothing from the Android framework so the behaviour
 * (recursion, conflict handling, cancellation and progress accounting) can be exercised by
 * plain JVM unit tests.
 */
class LocalFileOperations(private val onProgress: (OperationProgress) -> Unit = {}) {

    private var lastProgress: OperationProgress? = null

    suspend fun copy(sources: List<File>, destination: File): Result<Unit> = withContext(Dispatchers.IO) {
        val result = runCatchingCancellable {
            require(destination.isDirectory || destination.mkdirs()) { "Destination is unavailable" }
            sources.forEach { source ->
                require(source.exists()) { "Source file is missing: ${source.path}" }
                require(!source.isDirectory || !FileOperationPolicy.isInsideOrEqual(source, destination)) { "Cannot copy a folder into itself" }
            }
            val totalBytes = sources.sumOf(LocalFileOperations::recursiveSize)
            var completedBytes = 0L
            sources.forEachIndexed { index, source ->
                coroutineContext.ensureActive()
                val target = conflictTarget(source, destination)
                copyRecursive(source, target) { delta ->
                    completedBytes += delta
                    emit(OperationProgress("copy", source.name, completedBytes, totalBytes, index, sources.size))
                }
            }
            emit(OperationProgress("copy", "", totalBytes, totalBytes, sources.size, sources.size, isComplete = true))
        }
        if (result.isFailure) emitFailure("copy", result.exceptionOrNull()?.message)
        result
    }

    suspend fun move(sources: List<File>, destination: File): Result<Unit> = withContext(Dispatchers.IO) {
        val result = runCatchingCancellable {
            require(destination.isDirectory || destination.mkdirs()) { "Destination is unavailable" }
            sources.forEach { source -> require(source.exists()) { "Source file is missing: ${source.path}" } }
            val totalBytes = sources.sumOf(LocalFileOperations::recursiveSize)
            var completedBytes = 0L
            sources.forEachIndexed { index, source ->
                coroutineContext.ensureActive()
                require(!source.isDirectory || !FileOperationPolicy.isInsideOrEqual(source, destination)) { "Cannot move a folder into itself" }
                val target = conflictTarget(source, destination)
                if (source.renameTo(target)) {
                    completedBytes += recursiveSize(target)
                    emit(OperationProgress("move", source.name, completedBytes, totalBytes, index + 1, sources.size))
                } else {
                    copyRecursive(source, target) { delta ->
                        completedBytes += delta
                        emit(OperationProgress("move", source.name, completedBytes, totalBytes, index, sources.size))
                    }
                    require(deleteRecursively(source)) { "Could not remove the original after copying" }
                }
            }
            emit(OperationProgress("move", "", totalBytes, totalBytes, sources.size, sources.size, isComplete = true))
        }
        if (result.isFailure) emitFailure("move", result.exceptionOrNull()?.message)
        result
    }

    suspend fun createFolder(parent: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        runCatchingCancellable {
            val target = File(parent, FileOperationPolicy.safeName(name))
            require(target.mkdirs()) { "Could not create folder" }
            target
        }
    }

    suspend fun createFile(parent: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        runCatchingCancellable {
            val target = File(parent, FileOperationPolicy.safeName(name))
            require(target.createNewFile()) { "File already exists" }
            target
        }
    }

    suspend fun rename(source: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        runCatchingCancellable {
            require(source.exists()) { "Source file is missing: ${source.path}" }
            val parent = source.parentFile ?: error("No parent")
            val target = File(parent, FileOperationPolicy.safeName(name))
            require(!target.exists()) { "A file with this name already exists" }
            require(source.renameTo(target)) { "Rename failed" }
            target
        }
    }

    /** Conflict-safe target inside [destination]; never overwrites an existing file. */
    fun conflictTarget(source: File, destination: File): File = FileOperationPolicy.conflictSafe(File(destination, source.name))

    private suspend fun copyRecursive(source: File, target: File, onBytes: (Long) -> Unit) {
        coroutineContext.ensureActive()
        if (source.isDirectory) {
            require(target.mkdirs() || target.isDirectory) { "Could not create ${target.path}" }
            source.listFiles()?.forEach { child -> copyRecursive(child, File(target, child.name), onBytes) }
        } else {
            target.parentFile?.mkdirs()
            FileInputStream(source).use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read: Int
                    while (input.read(buffer).also { read = it } >= 0) {
                        coroutineContext.ensureActive()
                        if (read > 0) {
                            output.write(buffer, 0, read)
                            onBytes(read.toLong())
                        }
                    }
                }
            }
        }
    }

    private fun emit(progress: OperationProgress) {
        lastProgress = progress
        onProgress(progress)
    }

    private fun emitFailure(operation: String, message: String?) {
        val previous = lastProgress
        emit(
            previous?.copy(operation = operation, error = message, isComplete = true)
                ?: OperationProgress(operation, "", 0, 0, 0, 0, isComplete = true, error = message)
        )
    }

    companion object {
        fun recursiveSize(file: File): Long =
            if (file.isFile) file.length() else file.listFiles()?.sumOf(::recursiveSize) ?: 0L

        fun deleteRecursively(file: File): Boolean = file.deleteRecursively()
    }
}
