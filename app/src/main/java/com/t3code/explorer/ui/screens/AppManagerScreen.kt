package com.t3code.explorer.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.t3code.explorer.R
import com.t3code.explorer.data.apps.ApplicationRepository
import com.t3code.explorer.data.apps.InstalledApp
import com.t3code.explorer.domain.util.readableFileSize
import coil.compose.rememberDrawablePainter

@Composable
fun AppManagerScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { ApplicationRepository(context) }
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    LaunchedEffect(Unit) { apps = repository.list() }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp)) {
        items(apps, key = { it.packageName }) { app ->
            ListItem(headlineContent = { Text(app.label) }, supportingContent = { Text("${app.packageName} • ${app.versionName} • ${app.apkSize.readableFileSize()}") }, leadingContent = { Icon(rememberDrawablePainter(app.icon), app.label) }, modifier = Modifier.clickable { app.launchIntent?.let(context::startActivity) })
        }
    }
}
