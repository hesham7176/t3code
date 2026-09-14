package com.t3code.explorer.data.files

import android.content.Context
import com.t3code.explorer.domain.model.RecycleEntry
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecycleBinRepository(private val context: Context) {
    suspend fun list(): List<RecycleEntry> = withContext(Dispatchers.IO) {
        val bin = File(context.filesDir, "recycle-bin")
        bin.listFiles()?.asSequence()
            ?.filter { !it.name.endsWith(".meta") }
            ?.map { item ->
                val original = File(item.parentFile, "${item.name}.meta").takeIf { it.exists() }?.readText().orEmpty()
                RecycleEntry(item.name, original, item.absolutePath, item.lastModified(), if (item.isFile) item.length() else 0, item.name.substringAfterLast('_'))
            }?.sortedByDescending { it.deletedAt }?.toList().orEmpty()
    }

    suspend fun empty(): Result<Unit> = withContext(Dispatchers.IO) { runCatching { File(context.filesDir, "recycle-bin").deleteRecursively() } }
}
