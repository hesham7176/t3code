package com.t3code.explorer.data.downloads

import java.net.HttpURLConnection
import java.net.URL

object DownloadPolicy {
    fun requireSupported(url: URL) {
        require(url.protocol == "http" || url.protocol == "https") { "Only HTTP(S) downloads are supported" }
    }

    fun shouldAppend(partialBytes: Long, responseCode: Int): Boolean =
        partialBytes > 0 && responseCode == HttpURLConnection.HTTP_PARTIAL

    fun totalBytes(startOffset: Long, responseLength: Long, appending: Boolean): Long =
        if (responseLength < 0) -1L else (if (appending) startOffset else 0L) + responseLength
}
