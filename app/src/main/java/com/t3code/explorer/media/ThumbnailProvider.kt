package com.t3code.explorer.media

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.SuccessResult
import android.content.Context
import java.io.File

class ThumbnailProvider(private val context: Context) {
    private val loader = ImageLoader.Builder(context).components { add(VideoFrameDecoder.Factory()) }.build()

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
