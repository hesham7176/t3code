package com.t3code.explorer

import android.app.Application
import com.t3code.explorer.data.files.FileOperationManager
import com.t3code.explorer.data.files.FileRepository
import com.t3code.explorer.data.files.RecycleBinRepository
import com.t3code.explorer.data.preferences.PreferencesRepository
import com.t3code.explorer.data.storage.StorageRepository
import com.t3code.explorer.media.MediaEngine
import com.t3code.explorer.media.PlaybackPositionStore
import com.t3code.explorer.media.ThumbnailProvider

class ExplorerApplication : Application() {
    val preferences by lazy { PreferencesRepository(this) }
    val files by lazy { FileRepository(this) }
    val storage by lazy { StorageRepository(this) }
    val operations by lazy { FileOperationManager(this) }
    val recycleBin by lazy { RecycleBinRepository(this) }
    val playbackPositions by lazy { PlaybackPositionStore(this) }
    val thumbnailProvider by lazy { ThumbnailProvider(this) }
    val mediaEngine by lazy { MediaEngine(this) }
}
