package com.t3code.explorer.media

import android.app.Activity
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaybackPositionStore(context: Context) {
    private val file = File(context.filesDir, "playback_positions.properties")

    suspend fun get(path: String): Long = withContext(Dispatchers.IO) {
        val properties = load()
        properties.getProperty(path)?.toLongOrNull() ?: 0L
    }

    suspend fun put(path: String, position: Long) = withContext(Dispatchers.IO) {
        val properties = load()
        properties.setProperty(path, position.coerceAtLeast(0L).toString())
        file.parentFile?.mkdirs()
        file.outputStream().use { properties.store(it, "Media playback positions") }
    }

    private fun load(): Properties = Properties().also { properties ->
        if (file.exists()) {
            file.inputStream().use { properties.load(it) }
        }
    }
}

class MediaLifecycleObserver(private val engine: MediaEngine) : DefaultLifecycleObserver {
    override fun onDestroy(owner: LifecycleOwner) {
        if (owner !is Activity || !owner.isChangingConfigurations) engine.release()
    }
}
