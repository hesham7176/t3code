package com.t3code.explorer.media

import android.content.Context
import android.net.Uri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaybackPositionStore(context: Context) {
    private val file = File(context.filesDir, "playback_positions.properties")

    suspend fun get(path: String): Long = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext 0L
        file.readLines().firstOrNull { it.substringBefore('=') == path }?.substringAfter('=')?.toLongOrNull() ?: 0L
    }

    suspend fun put(path: String, position: Long) = withContext(Dispatchers.IO) {
        val values = if (file.exists()) file.readLines().associate { it.substringBefore('=') to it.substringAfter('=').toLongOrNull().orZero() }.toMutableMap() else mutableMapOf()
        values[path] = position
        file.writeText(values.entries.joinToString("\n") { "${it.key}=${it.value}" })
    }

    private fun Long?.orZero() = this ?: 0L
}

class MediaLifecycleObserver(private val engine: MediaEngine) : DefaultLifecycleObserver {
    override fun onDestroy(owner: LifecycleOwner) { engine.release() }
}
