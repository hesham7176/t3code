package com.t3code.explorer.data.files

import android.content.Context
import com.t3code.explorer.domain.model.RecycleEntry
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecycleBinRepository(context: Context, binDirectory: File = File(context.filesDir, "recycle-bin")) {
    private val manager = RecycleBinManager(binDirectory)

    suspend fun list(): List<RecycleEntry> = withContext(Dispatchers.IO) { manager.entries() }

    suspend fun empty(): Result<Unit> = withContext(Dispatchers.IO) { manager.empty() }
}
