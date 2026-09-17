package com.t3code.explorer.data.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.t3code.explorer.domain.model.StorageLocation
import java.io.File

class StorageRepository(private val context: Context) {
    fun locations(): List<StorageLocation> {
        val result = mutableListOf<StorageLocation>()
        val shared = Environment.getExternalStorageDirectory()
        if (shared.exists()) result += shared.toLocation("internal", "Internal storage", false)
        context.getExternalFilesDir(null)?.let { appExternal ->
            if (result.none { it.path == appExternal.path }) result += appExternal.toLocation("app", "App storage", false)
        }
        context.getExternalFilesDirs(null).drop(1).forEachIndexed { index, file ->
            if (file != null && file.exists()) result += file.toLocation("removable-$index", "Removable storage ${index + 1}", true)
        }
        return result.distinctBy { it.path }
    }

    fun primary(): StorageLocation? = locations().firstOrNull()

    private fun File.toLocation(id: String, name: String, removable: Boolean): StorageLocation {
        val stats = StatFs(path)
        return StorageLocation(id, name, path, stats.totalBytes, stats.availableBytes, removable)
    }
}
