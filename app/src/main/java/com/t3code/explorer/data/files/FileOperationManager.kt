package com.t3code.explorer.data.files

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.t3code.explorer.domain.model.FileItem
import com.t3code.explorer.domain.model.OperationProgress
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Facade used by the UI layer.
 *
 * It owns the observable progress state and the Android specific bits (share intents, the bin
 * directory), while the actual file work lives in [LocalFileOperations] and [RecycleBinManager]
 * which are testable without a device.
 */
class FileOperationManager(context: Context, binDirectory: File = File(context.filesDir, "recycle-bin")) {
    private val local = LocalFileOperations { progress -> _progress.value = progress }
    private val bin = RecycleBinManager(binDirectory) { progress -> _progress.value = progress }
    private val appContext = context.applicationContext

    private val _progress = MutableStateFlow<OperationProgress?>(null)
    val progress: StateFlow<OperationProgress?> = _progress.asStateFlow()

    suspend fun copy(items: List<FileItem>, destination: File): Result<Unit> =
        local.copy(items.map { File(it.path) }, destination)

    suspend fun move(items: List<FileItem>, destination: File): Result<Unit> =
        local.move(items.map { File(it.path) }, destination)

    suspend fun createFolder(parent: File, name: String): Result<File> = local.createFolder(parent, name)
    suspend fun createFile(parent: File, name: String): Result<File> = local.createFile(parent, name)
    suspend fun rename(item: FileItem, name: String): Result<File> = local.rename(File(item.path), name)

    suspend fun deleteToRecycleBin(items: List<FileItem>): Result<Unit> =
        bin.delete(items.map { File(it.path) })

    suspend fun restoreRecycleEntry(entry: RecycleEntryRecord): Result<File> = bin.restore(entry)

    fun shareIntent(item: FileItem): Intent? {
        val file = File(item.path)
        val uri = item.uri ?: FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
        if (item.uri == null && !file.exists()) return null
        return Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
