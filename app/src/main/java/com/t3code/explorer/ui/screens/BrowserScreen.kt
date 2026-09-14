package com.t3code.explorer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.t3code.explorer.R
import com.t3code.explorer.domain.model.FileItem
import com.t3code.explorer.domain.model.SortDirection
import com.t3code.explorer.domain.model.SortField
import com.t3code.explorer.domain.model.ViewMode
import com.t3code.explorer.domain.util.readableFileSize
import com.t3code.explorer.ui.ExplorerUiState
import com.t3code.explorer.ui.ExplorerViewModel
import com.t3code.explorer.ui.util.formatDate
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BrowserScreen(
    state: ExplorerUiState,
    padding: PaddingValues,
    viewModel: ExplorerViewModel,
    onOpenMedia: (FileItem) -> Unit,
    onSearch: () -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showView by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var createFile by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileItem?>(null) }

    Column(Modifier.fillMaxSize().padding(padding)) {
        TopAppBar(
            title = { Text(if (state.selected.isNotEmpty()) "${state.selected.size}" else stringResource(R.string.file_manager)) },
            navigationIcon = {
                IconButton(onClick = viewModel::navigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            },
            actions = {
                if (state.selected.isNotEmpty()) {
                    IconButton(onClick = { showDelete = true }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                    IconButton(onClick = viewModel::clearSelection) { Icon(Icons.Default.Close, stringResource(R.string.cancel)) }
                } else {
                    IconButton(onClick = onSearch) { Icon(Icons.Default.Search, stringResource(R.string.search)) }
                    IconButton(onClick = { showSort = true }) { Icon(Icons.Default.Sort, stringResource(R.string.sort)) }
                    IconButton(onClick = { showView = true }) { Icon(if (state.preferences.viewMode.isGrid) Icons.Default.ViewList else Icons.Default.GridView, stringResource(R.string.view)) }
                    IconButton(onClick = { showCreate = true }) { Icon(Icons.Default.Add, stringResource(R.string.create)) }
                    IconButton(onClick = { viewModel.loadDirectory() }) { Icon(Icons.Default.Refresh, stringResource(R.string.refresh)) }
                }
            }
        )
        Breadcrumbs(state.currentPath, onPath = viewModel::loadDirectory)
        state.operation?.takeIf { !it.isComplete }?.let { operation ->
            LinearProgressIndicator({ operation.percent / 100f }, Modifier.fillMaxWidth())
        }
        if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.items.isEmpty() && !state.isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.empty_folder), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else if (state.preferences.viewMode.isGrid) {
            val cells = when (state.preferences.viewMode) { ViewMode.SMALL_GRID -> 5; ViewMode.LARGE_GRID -> 2; else -> 3 }
            LazyVerticalGrid(GridCells.Fixed(cells), Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.items, key = { it.path }) { item ->
                    FileGridCard(item, item.path in state.selected, onOpen = { if (item.isDirectory) viewModel.loadDirectory(item.path) else onOpenMedia(item) }, onSelect = { viewModel.toggleSelection(item) }, onRename = { renameTarget = item }, onDelete = { viewModel.toggleSelection(item); showDelete = true }, onShare = { viewModel.share(item) })
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.items, key = { it.path }) { item ->
                    FileListRow(item, item.path in state.selected, state.preferences.viewMode.detailLines > 0, onOpen = { if (item.isDirectory) viewModel.loadDirectory(item.path) else onOpenMedia(item) }, onSelect = { viewModel.toggleSelection(item) }, onRename = { renameTarget = item }, onDelete = { viewModel.toggleSelection(item); showDelete = true }, onShare = { viewModel.share(item) })
                }
            }
        }
    }

    if (showCreate) CreateDialog(onDismiss = { showCreate = false }, onFolder = { name -> showCreate = false; viewModel.createFolder(name) }, onFile = { name -> showCreate = false; viewModel.createFile(name) })
    if (showDelete) AlertDialog(onDismissRequest = { showDelete = false }, title = { Text(stringResource(R.string.delete)) }, text = { Text(stringResource(R.string.confirm_delete)) }, confirmButton = { TextButton(onClick = { showDelete = false; viewModel.deleteSelected() }) { Text(stringResource(R.string.delete)) } }, dismissButton = { TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.cancel)) } })
    renameTarget?.let { target -> NameDialog(stringResource(R.string.rename), target.name, { renameTarget = null }, { value -> renameTarget = null; viewModel.rename(target, value) }) }
    if (showSort) SortDialog(state, { showSort = false }, viewModel)
    if (showView) ViewDialog(state.preferences.viewMode, { showView = false }, viewModel)
}

@Composable
private fun Breadcrumbs(path: String, onPath: (String) -> Unit) {
    val parts = path.split(File.separator).filter { it.isNotBlank() }
    Row(Modifier.fillMaxWidth().horizontalScrollIfNeeded().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("/", color = MaterialTheme.colorScheme.primary)
        var built = ""
        parts.forEach { part ->
            built += File.separator + part
            Text("  /  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(part, modifier = Modifier.combinedClickable(onClick = { onPath(built) }, onLongClick = {}), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FileGridCard(item: FileItem, selected: Boolean, onOpen: () -> Unit, onSelect: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit, onShare: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onOpen, onLongClick = onSelect), colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Column {
            Box(Modifier.fillMaxWidth().height(104.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                Preview(item, Modifier.fillMaxSize())
                IconButton({ menu = true }, Modifier.align(Alignment.TopEnd)) { Icon(Icons.Default.MoreVert, null) }
                if (selected) Icon(Icons.Default.Check, null, Modifier.align(Alignment.TopStart).padding(8.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.padding(8.dp)) {
                Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                Text(if (item.isDirectory) stringResource(R.string.files_count, item.childCount ?: 0) else item.size.readableFileSize(), maxLines = 1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    ItemMenu(menu, { menu = false }, { menu = false; onOpen() }, { menu = false; onRename() }, { menu = false; onDelete() }, { menu = false; onShare() })
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FileListRow(item: FileItem, selected: Boolean, details: Boolean, onOpen: () -> Unit, onSelect: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit, onShare: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = onOpen, onLongClick = onSelect).background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Preview(item, Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Text(if (details) "${item.size.readableFileSize()}  •  ${formatDate(item.modifiedAt)}" else if (item.isDirectory) stringResource(R.string.files_count, item.childCount ?: 0) else item.size.readableFileSize(), maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, null) }
    }
    ItemMenu(menu, { menu = false }, { menu = false; onOpen() }, { menu = false; onRename() }, { menu = false; onDelete() }, { menu = false; onShare() })
}

@Composable
private fun Preview(item: FileItem, modifier: Modifier) {
    val model = item.coverPath ?: if (!item.isDirectory && (item.mimeType.startsWith("image/") || item.mimeType.startsWith("video/"))) item.path else null
    if (model != null) AsyncImage(model, item.name, modifier, contentScale = ContentScale.Crop)
    else Icon(when {
        item.isDirectory -> Icons.Default.Folder
        item.mimeType.startsWith("video/") -> Icons.Default.Movie
        item.mimeType.startsWith("audio/") -> Icons.Default.AudioFile
        item.mimeType.startsWith("image/") -> Icons.Default.Image
        else -> Icons.Default.Description
    }, item.name, modifier = Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ItemMenu(expanded: Boolean, onDismiss: () -> Unit, onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit, onShare: () -> Unit) {
    DropdownMenu(expanded, onDismiss) {
        DropdownMenuItem({ Text(stringResource(R.string.open)) }, onOpen)
        DropdownMenuItem({ Text(stringResource(R.string.rename)) }, onRename)
        DropdownMenuItem({ Text(stringResource(R.string.delete)) }, onDelete)
        DropdownMenuItem({ Text(stringResource(R.string.share)) }, onShare)
    }
}

@Composable
private fun CreateDialog(onDismiss: () -> Unit, onFolder: (String) -> Unit, onFile: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.create), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value, { value = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { if (value.isNotBlank()) onFolder(value) }) { Text(stringResource(R.string.new_folder)) }
                TextButton(onClick = { if (value.isNotBlank()) onFile(value) }) { Text(stringResource(R.string.new_file)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        } }
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(value, { value = it }, singleLine = true) }, confirmButton = { TextButton({ if (value.isNotBlank()) onSubmit(value) }) { Text(stringResource(R.string.save)) } }, dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun SortDialog(state: ExplorerUiState, onDismiss: () -> Unit, viewModel: ExplorerViewModel) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.sort)) }, text = {
        Column { SortField.values().forEach { field -> Row(Modifier.fillMaxWidth().clickable { viewModel.setSort(field, state.preferences.sortDirection); onDismiss() }, verticalAlignment = Alignment.CenterVertically) { RadioButton(state.preferences.sortField == field, null); Text(field.name.lowercase().replaceFirstChar { it.uppercase() }) } }; Row(verticalAlignment = Alignment.CenterVertically) { TextButton({ viewModel.setSort(state.preferences.sortField, if (state.preferences.sortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING); onDismiss() }) { Text(if (state.preferences.sortDirection == SortDirection.ASCENDING) stringResource(R.string.descending) else stringResource(R.string.ascending)) } } }
    }, confirmButton = {})
}

@Composable
private fun ViewDialog(current: ViewMode, onDismiss: () -> Unit, viewModel: ExplorerViewModel) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.view)) }, text = { Column { ViewMode.values().forEach { mode -> Row(Modifier.fillMaxWidth().clickable { viewModel.setViewMode(mode); onDismiss() }, verticalAlignment = Alignment.CenterVertically) { RadioButton(current == mode, null); Text(mode.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) } } } }, confirmButton = {})
}

private fun Modifier.horizontalScrollIfNeeded() = this
