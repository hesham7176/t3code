package com.t3code.explorer.media

import android.media.MediaMetadataRetriever
import java.io.File

data class MediaMetadata(val title: String, val artist: String, val album: String, val durationMs: Long, val width: Int?, val height: Int?, val bitrate: String?, val codec: String?)

class MediaMetadataReader {
    fun read(file: File): MediaMetadata? {
        val retriever = MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(file.absolutePath)
            MediaMetadata(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.nameWithoutExtension,
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty(),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty(),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0,
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull(),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            )
        }.getOrNull().also { retriever.release() }
    }
}
