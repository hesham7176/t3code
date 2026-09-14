package com.t3code.explorer.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(val label: String, val packageName: String, val versionName: String, val icon: Drawable, val apkSize: Long, val launchIntent: Intent?)

class ApplicationRepository(private val context: Context) {
    suspend fun list(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val manager = context.packageManager
        manager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != context.packageName }
            .map { info ->
                val packageInfo = manager.getPackageInfo(info.packageName, 0)
                InstalledApp(info.loadLabel(manager).toString(), info.packageName, packageInfo.versionName.orEmpty(), info.loadIcon(manager), info.sourceDir?.let { java.io.File(it).length() } ?: 0L, manager.getLaunchIntentForPackage(info.packageName))
            }.sortedBy { it.label.lowercase() }
    }

    fun launch(app: InstalledApp) { app.launchIntent?.let(context::startActivity) }
    fun detailsIntent(app: InstalledApp) = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${app.packageName}"))
}
