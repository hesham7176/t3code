package com.t3code.explorer.data.downloads

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val urlText = inputData.getString(KEY_URL) ?: return Result.failure()
        val destination = inputData.getString(KEY_DESTINATION) ?: return Result.failure()
        return try {
            download(urlText, File(destination))
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun download(urlText: String, destination: File) {
        val url = URL(urlText)
        require(url.protocol == "http" || url.protocol == "https") { "Only HTTP(S) downloads are supported" }
        val partial = File("${destination.absolutePath}.part")
        val offset = partial.length()
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            if (offset > 0) setRequestProperty("Range", "bytes=$offset-")
        }
        try {
            connection.connect()
            val response = connection.responseCode
            require(response in 200..299) { "HTTP $response" }
            val append = offset > 0 && response == HttpURLConnection.HTTP_PARTIAL
            val completedAtStart = if (append) offset else 0L
            if (!append && offset > 0) partial.delete()
            val contentLength = connection.contentLengthLong
            val total = if (contentLength >= 0) completedAtStart + contentLength else -1L
            partial.parentFile?.mkdirs()
            FileOutputStream(partial, append).use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var completed = completedAtStart
                    var read: Int
                    while (input.read(buffer).also { read = it } >= 0) {
                        coroutineContext.ensureActive()
                        if (read > 0) {
                            output.write(buffer, 0, read)
                            completed += read
                            setProgress(workDataOf(KEY_COMPLETED to completed, KEY_TOTAL to total))
                        }
                    }
                }
            }
            require(partial.renameTo(destination) || (destination.delete() && partial.renameTo(destination))) { "Could not finalize download" }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_DESTINATION = "destination"
        const val KEY_COMPLETED = "completed"
        const val KEY_TOTAL = "total"
    }
}
