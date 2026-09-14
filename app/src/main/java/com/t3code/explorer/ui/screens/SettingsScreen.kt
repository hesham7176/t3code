package com.t3code.explorer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.t3code.explorer.R
import com.t3code.explorer.ui.ExplorerUiState
import com.t3code.explorer.ui.ExplorerViewModel

@Composable
fun SettingsScreen(state: ExplorerUiState, padding: PaddingValues, viewModel: ExplorerViewModel, onOpenStorageSettings: () -> Unit) {
    var themeDialog by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item { Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(20.dp)) }
        item { Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) }
        item { SettingRow(Icons.Default.DarkMode, stringResource(R.string.theme), state.preferences.theme.replaceFirstChar { it.uppercase() }, { themeDialog = true }) }
        item { SettingRow(Icons.Default.Language, stringResource(R.string.language), stringResource(R.string.english), {}) }
        item { HorizontalDivider() }
        item { Text(stringResource(R.string.file_manager), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) }
        item { SwitchRow(Icons.Default.Folder, stringResource(R.string.folder_covers), state.preferences.folderCovers, viewModel::setFolderCovers) }
        item { SwitchRow(Icons.Default.Folder, stringResource(R.string.show_hidden), state.preferences.showHidden, viewModel::setShowHidden) }
        item { SettingRow(Icons.Default.Storage, stringResource(R.string.storage), stringResource(R.string.storage_analyzer), onOpenStorageSettings) }
        item { HorizontalDivider() }
        item { Text(stringResource(R.string.media_player), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) }
        item { SwitchRow(Icons.Default.PlayCircle, stringResource(R.string.resume_playback), state.preferences.resumePlayback, viewModel::setResumePlayback) }
        item { SwitchRow(Icons.Default.PlayCircle, stringResource(R.string.background_playback), state.preferences.backgroundPlayback, viewModel::setBackgroundPlayback) }
        item { Text(stringResource(R.string.version, "0.1.0"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (themeDialog) {
        AlertDialog(onDismissRequest = { themeDialog = false }, title = { Text(stringResource(R.string.theme)) }, text = { Column { listOf("system" to R.string.system_default, "light" to R.string.light, "dark" to R.string.dark).forEach { (value, label) -> Row(Modifier.fillMaxWidth().clickable { viewModel.setTheme(value); themeDialog = false }.padding(vertical = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { RadioButton(state.preferences.theme == value, null); Text(stringResource(label)) } } } }, confirmButton = { TextButton({ themeDialog = false }) { Text(stringResource(R.string.close)) } })
    }
}

@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(title) }, supportingContent = { Text(subtitle) }, leadingContent = { Icon(icon, null) }, modifier = Modifier.clickable(onClick = onClick))
}

@Composable
private fun SwitchRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    ListItem(headlineContent = { Text(title) }, leadingContent = { Icon(icon, null) }, trailingContent = { Switch(checked, onCheckedChange = onChecked) })
}
