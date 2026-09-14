package com.t3code.explorer.data.playlist

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaylistRepository(context: Context) {
    private val root = File(context.filesDir, "playlists").apply { mkdirs() }

    suspend fun list(name: String): List<String> = withContext(Dispatchers.IO) { file(name).takeIf { it.exists() }?.readLines().orEmpty() }
    suspend fun save(name: String, paths: List<String>) = withContext(Dispatchers.IO) { file(name).writeText(paths.distinct().joinToString("\n")) }
    suspend fun delete(name: String) = withContext(Dispatchers.IO) { file(name).delete() }
    private fun file(name: String) = File(root, name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "playlist" })
}
