package com.t3code.explorer.data.downloads

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val urlText = inputData.getString(KEY_URL) ?: return Result.failure()
        val destination = inputData.getString(KEY_DESTINATION) ?: return Result.failure()
        return runCatching {
            val connection = URL(urlText).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            connection.connect()
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            val total = connection.contentLengthLong
            File(destination).parentFile?.mkdirs()
            connection.inputStream.use { input -> File(destination).outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var completed = 0L
                var read: Int
                while (input.read(buffer).also { read = it } >= 0) {
                    coroutineContext.ensureActive()
                    if (read > 0) {
                        output.write(buffer, 0, read)
                        completed += read
                        setProgress(workDataOf(KEY_COMPLETED to completed, KEY_TOTAL to total))
                    }
                }
            }}
            Result.success()
        }.getOrElse { if (runAttemptCount < 3) Result.retry() else Result.failure() }
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_DESTINATION = "destination"
        const val KEY_COMPLETED = "completed"
        const val KEY_TOTAL = "total"
    }
}
