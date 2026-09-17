package com.t3code.explorer.data.downloads

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.io.File
import java.util.UUID

class DownloadRepository(context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun enqueue(url: String, destination: File): UUID {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(Data.Builder().putString(DownloadWorker.KEY_URL, url).putString(DownloadWorker.KEY_DESTINATION, destination.absolutePath).build())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueue(request)
        return request.id
    }

    fun observe(id: UUID) = workManager.getWorkInfoByIdLiveData(id)
    fun pause(id: UUID) = workManager.cancelWorkById(id)
    fun cancel(id: UUID) = workManager.cancelWorkById(id)
    fun status(info: WorkInfo): DownloadStatus = DownloadStatus(info.id, info.state, info.progress.getLong(DownloadWorker.KEY_COMPLETED, 0L), info.progress.getLong(DownloadWorker.KEY_TOTAL, -1L))
}

data class DownloadStatus(val id: UUID, val state: WorkInfo.State, val completed: Long, val total: Long)
