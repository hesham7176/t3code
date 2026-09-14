package com.t3code.explorer.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.t3code.explorer.R
import com.t3code.explorer.ui.screens.AnalyzerScreen
import com.t3code.explorer.ui.screens.BrowserScreen
import com.t3code.explorer.ui.screens.HomeScreen
import com.t3code.explorer.ui.screens.MediaScreen
import com.t3code.explorer.ui.screens.SearchScreen
import com.t3code.explorer.ui.screens.SettingsScreen

private enum class Destination { HOME, BROWSE, SEARCH, SETTINGS, ANALYZER, MEDIA }

@Composable
fun ExplorerApp(viewModel: ExplorerViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    var mediaPath by rememberSaveable { mutableStateOf("") }
    var mediaIsVideo by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.message, state.error) {
        val message = state.message ?: state.error
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(state.shareIntent) {
        state.shareIntent?.let { intent ->
            context.startActivity(Intent.createChooser(intent, null))
            viewModel.consumeShareIntent()
        }
    }

    val showBottomBar = destination != Destination.MEDIA
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBottomBar) NavigationBar {
                NavigationBarItem(destination == Destination.HOME, { destination = Destination.HOME }, icon = { Icon(Icons.Default.Home, null) }, label = { androidx.compose.material3.Text(stringResource(R.string.home)) })
                NavigationBarItem(destination == Destination.BROWSE, { destination = Destination.BROWSE }, icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }, label = { androidx.compose.material3.Text(stringResource(R.string.browse)) })
                NavigationBarItem(destination == Destination.SEARCH, { destination = Destination.SEARCH }, icon = { Icon(Icons.Default.Search, null) }, label = { androidx.compose.material3.Text(stringResource(R.string.search)) })
                NavigationBarItem(destination == Destination.SETTINGS, { destination = Destination.SETTINGS }, icon = { Icon(Icons.Default.Settings, null) }, label = { androidx.compose.material3.Text(stringResource(R.string.settings)) })
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            when (destination) {
                Destination.HOME -> HomeScreen(state, padding, onBrowse = { destination = Destination.BROWSE; viewModel.loadDirectory() }, onOpenPath = { path -> viewModel.loadDirectory(path); destination = Destination.BROWSE }, onAnalyze = { destination = Destination.ANALYZER; viewModel.analyzeStorage() })
                Destination.BROWSE -> BrowserScreen(state, padding, viewModel, onOpenMedia = { item ->
                    mediaPath = item.path
                    mediaIsVideo = item.mimeType.startsWith("video/")
                    destination = Destination.MEDIA
                }, onSearch = { destination = Destination.SEARCH })
                Destination.SEARCH -> SearchScreen(state, padding, viewModel, onOpenMedia = { item -> mediaPath = item.path; mediaIsVideo = item.mimeType.startsWith("video/"); destination = Destination.MEDIA })
                Destination.SETTINGS -> SettingsScreen(state, padding, viewModel, onOpenStorageSettings = { context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) })
                Destination.ANALYZER -> AnalyzerScreen(state.analysis, state.isAnalyzing, padding)
                Destination.MEDIA -> MediaScreen(state, mediaPath, mediaIsVideo, onBack = { destination = Destination.BROWSE })
            }
        }
    }
}
