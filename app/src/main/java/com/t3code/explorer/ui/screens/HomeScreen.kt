package com.t3code.explorer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.t3code.explorer.R
import com.t3code.explorer.domain.model.FileCategory
import com.t3code.explorer.domain.model.StorageLocation
import com.t3code.explorer.ui.ExplorerUiState
import com.t3code.explorer.domain.util.readableFileSize

private data class CategoryTile(val title: Int, val icon: ImageVector, val category: FileCategory)

@Composable
fun HomeScreen(
    state: ExplorerUiState,
    padding: PaddingValues,
    onCategory: (FileCategory) -> Unit,
    onOpenPath: (String) -> Unit,
    onAnalyze: () -> Unit
) {
    val categories = listOf(
        CategoryTile(R.string.images, Icons.Default.Image, FileCategory.IMAGE), CategoryTile(R.string.videos, Icons.Default.Movie, FileCategory.VIDEO),
        CategoryTile(R.string.music, Icons.Default.AudioFile, FileCategory.AUDIO), CategoryTile(R.string.audio, Icons.Default.AudioFile, FileCategory.AUDIO),
        CategoryTile(R.string.documents, Icons.Default.Description, FileCategory.DOCUMENT), CategoryTile(R.string.applications, Icons.Default.InstallMobile, FileCategory.APK),
        CategoryTile(R.string.archives, Icons.Default.Archive, FileCategory.ARCHIVE), CategoryTile(R.string.downloads, Icons.Default.Download, FileCategory.OTHER)
    )
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.file_manager), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Text(stringResource(R.string.categories), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(190.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), userScrollEnabled = false) {
                items(categories) { tile ->
                    Card(onClick = { onCategory(tile.category) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(tile.icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(tile.title), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }
        }
        item { Text(stringResource(R.string.storage), style = MaterialTheme.typography.titleMedium) }
        items(state.locations.size) { index ->
            StorageCard(state.locations[index], onClick = { onOpenPath(state.locations[index].path) })
        }
        item {
            Card(onClick = onAnalyze, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.storage_analyzer), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.storage_used_percent, state.locations.firstOrNull()?.usagePercent ?: 0), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageCard(location: StorageLocation, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (location.isRemovable) Icons.Default.Folder else Icons.Default.Storage, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(location.name, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.used_space, location.usedBytes.readableFileSize(), location.totalBytes.readableFileSize()), style = MaterialTheme.typography.bodySmall)
                }
                Text("${location.usagePercent}%", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator({ location.usagePercent / 100f }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.free_space, location.freeBytes.readableFileSize()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
