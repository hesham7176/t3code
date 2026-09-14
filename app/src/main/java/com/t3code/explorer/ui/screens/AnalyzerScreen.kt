package com.t3code.explorer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.t3code.explorer.R
import com.t3code.explorer.domain.model.StorageAnalysis
import com.t3code.explorer.domain.util.readableFileSize

@Composable
fun AnalyzerScreen(analysis: StorageAnalysis?, loading: Boolean, padding: PaddingValues) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text(stringResource(R.string.storage_analyzer), style = MaterialTheme.typography.headlineSmall) }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        analysis?.let { result ->
            item { Text(stringResource(R.string.storage_summary, result.usedBytes.readableFileSize(), result.freeBytes.readableFileSize())) }
            item { Text(stringResource(R.string.largest_files), style = MaterialTheme.typography.titleMedium) }
            items(result.largestFiles) { file -> ListItem({ Text(file.name) }, supportingContent = { Text(file.size.readableFileSize()) }) }
        }
    }
}
