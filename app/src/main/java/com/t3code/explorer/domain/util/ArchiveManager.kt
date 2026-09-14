package com.t3code.explorer.domain.util

import com.t3code.explorer.domain.model.FileItem
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class ArchiveManager {
    suspend fun createZip(source: File, destination: File, onProgress: (Long) -> Unit = {}) {
        require(source.exists())
        ZipOutputStream(BufferedOutputStream(FileOutputStream(destination))).use { zip ->
            val base = if (source.isDirectory) source.parentFile else source.parentFile
            source.walkTopDown().filter { it != destination }.forEach { file ->
                coroutineContext.ensureActive()
                val relative = file.relativeTo(base).path.replace(File.separatorChar, '/')
                if (file.isDirectory) zip.putNextEntry(ZipEntry("$relative/")) else {
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
    }

    suspend fun extractZip(archive: File, destination: File, onProgress: (Long) -> Unit = {}) {
        require(archive.isFile)
        ZipFile(archive).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                coroutineContext.ensureActive()
                val target = File(destination, entry.name).canonicalFile
                require(target.path == destination.canonicalPath || target.path.startsWith(destination.canonicalPath + File.separator)) { "Unsafe archive entry" }
                if (entry.isDirectory) target.mkdirs() else {
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
}
