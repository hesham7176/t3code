package com.t3code.explorer.data.files

import android.content.Context
import com.t3code.explorer.domain.model.RecycleEntry
import java.io.File
import com.t3code.explorer.domain.util.RecycleMetadataCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecycleBinRepository(private val context: Context) {
    suspend fun list(): List<RecycleEntry> = withContext(Dispatchers.IO) {
        val bin = File(context.filesDir, "recycle-bin")
        bin.listFiles()?.asSequence()
            ?.filter { !it.name.endsWith(".meta") }
            ?.map { item ->
                val metadata = File(item.parentFile, "${item.name}.meta")
                val original = decodeOriginalPath(metadata)
                RecycleEntry(
                    id = item.name,
                    originalPath = original,
                    deletedPath = item.absolutePath,
                    deletedAt = item.lastModified(),
                    size = recursiveSize(item),
                    name = original.takeIf { it.isNotBlank() }?.let(::File)?.name ?: item.name
                )
            }?.sortedByDescending { it.deletedAt }?.toList().orEmpty()
    }

    suspend fun empty(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { File(context.filesDir, "recycle-bin").deleteRecursively(); Unit }
    }

    private fun decodeOriginalPath(metadata: File): String {
        if (!metadata.exists()) return ""
        val encoded = runCatching { metadata.readText() }.getOrDefault("")
        return RecycleMetadataCodec.decode(encoded)
    }

    private fun recursiveSize(file: File): Long =
        if (file.isFile) file.length() else file.listFiles()?.sumOf(::recursiveSize) ?: 0L
}
