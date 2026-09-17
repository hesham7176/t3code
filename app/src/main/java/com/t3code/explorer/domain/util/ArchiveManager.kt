package com.t3code.explorer.domain.util

import com.t3code.explorer.domain.model.FileItem
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class ArchiveManager {
    suspend fun createZip(source: File, destination: File, overwrite: Boolean = false, onProgress: (Long) -> Unit = {}) {
        val sourceFile = source.absoluteFile
        require(sourceFile.exists())
        val requestedDestination = destination.absoluteFile
        require(requestedDestination.canonicalPath != sourceFile.canonicalPath) { "Archive destination cannot be the source" }
        val finalDestination = if (overwrite) requestedDestination else FileOperationPolicy.conflictSafe(requestedDestination)
        val temporary = File(finalDestination.parentFile ?: error("Archive has no parent"), ".${finalDestination.name}.${UUID.randomUUID()}.part")
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(temporary))).use { zip ->
                val base = sourceFile.parentFile ?: sourceFile
                sourceFile.walkTopDown().filter { it != finalDestination && it != temporary }.forEach { file ->
                    coroutineContext.ensureActive()
                    val relative = file.relativeTo(base).path.replace(File.separatorChar, '/')
                    if (file.isDirectory) {
                        zip.putNextEntry(ZipEntry("$relative/"))
                    } else {
                        zip.putNextEntry(ZipEntry(relative))
                        FileInputStream(file).use { input ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var read: Int
                            while (input.read(buffer).also { read = it } >= 0) {
                                coroutineContext.ensureActive()
                                if (read > 0) { zip.write(buffer, 0, read); onProgress(read.toLong()) }
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
            moveIntoPlace(temporary, finalDestination, overwrite)
        } finally {
            temporary.delete()
        }
    }

    suspend fun extractZip(archive: File, destination: File, overwrite: Boolean = false, onProgress: (Long) -> Unit = {}) {
        require(archive.isFile)
        val canonicalDestination = destination.canonicalFile
        require(destination.mkdirs() || destination.isDirectory) { "Could not create extraction directory" }
        ZipFile(archive).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                coroutineContext.ensureActive()
                val requested = File(canonicalDestination, entry.name).canonicalFile
                require(FileOperationPolicy.isInsideOrEqual(canonicalDestination, requested)) { "Unsafe archive entry" }
                val target = if (overwrite || entry.isDirectory) requested else FileOperationPolicy.conflictSafe(requested)
                if (entry.isDirectory) {
                    require(target.mkdirs() || target.isDirectory) { "Could not create extraction directory" }
                } else {
                    target.parentFile?.mkdirs()
                    BufferedInputStream(zip.getInputStream(entry)).use { input ->
                        FileOutputStream(target).use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var read: Int
                            while (input.read(buffer).also { read = it } >= 0) {
                                coroutineContext.ensureActive()
                                if (read > 0) { output.write(buffer, 0, read); onProgress(read.toLong()) }
                            }
                        }
                    }
                }
            }
        }
    }

    fun list(archive: File): List<FileItem> = ZipFile(archive).use { zip ->
        zip.entries().asSequence().map { entry ->
            val name = entry.name.substringAfterLast('/').ifEmpty { entry.name }
            FileItem(name, entry.name, isDirectory = entry.isDirectory, size = entry.size.coerceAtLeast(0), modifiedAt = entry.time, mimeType = FileType.mimeType(name), extension = FileType.extension(name))
        }.toList()
    }

    private fun moveIntoPlace(source: File, destination: File, overwrite: Boolean) {
        destination.parentFile?.mkdirs()
        runCatching {
            val options = buildList {
                if (overwrite) add(StandardCopyOption.REPLACE_EXISTING)
                add(StandardCopyOption.ATOMIC_MOVE)
            }.toTypedArray()
            Files.move(source.toPath(), destination.toPath(), *options)
        }.getOrElse {
            if (overwrite) require(source.renameTo(destination) || (destination.delete() && source.renameTo(destination))) { "Could not finalize archive" }
            else require(source.renameTo(destination)) { "Could not finalize archive" }
        }
    }
}
