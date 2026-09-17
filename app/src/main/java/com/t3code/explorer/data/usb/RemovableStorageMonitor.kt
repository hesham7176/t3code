package com.t3code.explorer.data.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RemovableStorageMonitor(context: Context) {
    private val manager = context.getSystemService(StorageManager::class.java)
    private val _volumes = MutableStateFlow<List<StorageVolume>>(emptyList())
    val volumes: StateFlow<List<StorageVolume>> = _volumes.asStateFlow()

    fun refresh() { _volumes.value = manager.storageVolumes.filter { it.isRemovable } }
}

class StorageVolumeReceiver(private val onChanged: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { if (intent.action in setOf(Intent.ACTION_MEDIA_MOUNTED, Intent.ACTION_MEDIA_UNMOUNTED, Intent.ACTION_MEDIA_REMOVED)) onChanged() }
}
