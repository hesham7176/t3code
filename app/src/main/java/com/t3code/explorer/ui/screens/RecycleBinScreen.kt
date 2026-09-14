package com.t3code.explorer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.t3code.explorer.R
import com.t3code.explorer.domain.model.RecycleEntry
import com.t3code.explorer.ui.ExplorerUiState
import com.t3code.explorer.ui.ExplorerViewModel

@Composable
fun RecycleBinScreen(state: ExplorerUiState, padding: PaddingValues, viewModel: ExplorerViewModel) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.recycle_bin))
            IconButton(viewModel::emptyRecycleBin) { Icon(Icons.Default.DeleteForever, stringResource(R.string.delete)) }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.recycleEntries, key = { it.id }) { entry -> RecycleRow(entry, viewModel::restore) }
        }
    }
}

@Composable
private fun RecycleRow(entry: RecycleEntry, onRestore: (RecycleEntry) -> Unit) {
    ListItem(headlineContent = { Text(entry.name) }, supportingContent = { Text(entry.originalPath) }, trailingContent = { IconButton({ onRestore(entry) }) { Icon(Icons.Default.Restore, stringResource(R.string.restore)) } })
}
