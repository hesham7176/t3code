package com.t3code.explorer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.t3code.explorer.R
import com.t3code.explorer.domain.model.FileItem
import com.t3code.explorer.ui.ExplorerUiState
import com.t3code.explorer.ui.ExplorerViewModel

@Composable
fun SearchScreen(state: ExplorerUiState, padding: PaddingValues, viewModel: ExplorerViewModel, onOpenMedia: (FileItem) -> Unit) {
    var query by remember { mutableStateOf(state.searchQuery) }
    LaunchedEffect(query) { if (query.isNotBlank()) viewModel.search(query) else viewModel.clearSearch() }
    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primary)
            OutlinedTextField(query, { query = it }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.search)) }, trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Default.Clear, null) } })
        }
        if (state.isSearching) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!state.isSearching && query.isNotBlank() && state.searchResults.isEmpty()) Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(stringResource(R.string.no_results)) }
        else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.searchResults, key = { it.path }) { item ->
                FileListRowForSearch(item, onClick = { if (item.isDirectory) viewModel.loadDirectory(item.path) else onOpenMedia(item) })
            }
        }
    }
}

@Composable
private fun FileListRowForSearch(item: FileItem, onClick: () -> Unit) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(item.name) },
        supportingContent = { Text(item.path, maxLines = 1) },
        leadingContent = { androidx.compose.material3.Icon(if (item.isDirectory) Icons.Default.Search else Icons.Default.Search, null, Modifier.size(28.dp)) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp)
    )
}
