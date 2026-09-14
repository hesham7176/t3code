package com.t3code.explorer.ui

import android.Manifest
import android.os.Build

object PermissionController {
    fun sharedStoragePermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    } else emptyArray()
}
