package com.t3code.explorer.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.io.File

/** Shared thumbnail loader with bounded memory and disk caches. */
class ThumbnailProvider(private val context: Context) {
    private val loader = ImageLoader.Builder(context)
        .memoryCache { MemoryCache.Builder(context).maxSizePercent(0.20).build() }
        .diskCache { DiskCache.Builder().directory(File(context.cacheDir, "thumbnails")).maxSizeBytes(128L * 1024 * 1024).build() }
        .components { add(VideoFrameDecoder.Factory()) }
        .build()

    suspend fun videoFrame(file: File): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(file.absolutePath)
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }.getOrNull().also { retriever.release() }
    }

    suspend fun imageDrawable(file: File) = runCatching {
        val request = ImageRequest.Builder(context).data(file).allowHardware(false).build()
        (loader.execute(request) as? SuccessResult)?.drawable
    }.getOrNull()
}
