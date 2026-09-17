package com.t3code.explorer.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.t3code.explorer.R
import com.t3code.explorer.data.text.TextDocumentRepository
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(path: String, padding: PaddingValues, onBack: () -> Unit) {
    var content by remember(path) { mutableStateOf("") }
    var error by remember(path) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val repository = remember(context) { TextDocumentRepository(context) }
    val scope = rememberCoroutineScope()
    val isSafDocument = path.startsWith("content:")
    val uri = remember(path) { if (isSafDocument) android.net.Uri.parse(path) else null }
    LaunchedEffect(path) {
        val result = if (uri != null) repository.read(uri) else repository.read(File(path))
        result.fold({ content = it; error = null }, { error = it.message })
    }
    Column(Modifier.fillMaxSize().padding(padding)) {
        TopAppBar(title = { Text(uri?.lastPathSegment?.substringAfterLast(':') ?: File(path).name) }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }, actions = { Button(onClick = { scope.launch { val result = if (uri != null) repository.write(uri, content) else repository.write(File(path), content); result.onFailure { error = it.message } } }) { Text(stringResource(R.string.save)) } })
        OutlinedTextField(content, { content = it }, Modifier.fillMaxWidth().weight(1f).padding(12.dp), isError = error != null)
        error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
    }
}
