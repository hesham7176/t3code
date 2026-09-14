package com.t3code.explorer.ui.screens

import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.t3code.explorer.ExplorerApplication
import com.t3code.explorer.domain.util.FileType
import com.t3code.explorer.ui.ExplorerUiState
import com.t3code.explorer.ui.util.formatDuration
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun MediaScreen(state: ExplorerUiState, mediaPath: String, isVideo: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ExplorerApplication
    val engine = app.mediaEngine
    val playback by engine.state.collectAsStateWithLifecycle()
    val uri = remember(mediaPath) { if (mediaPath.startsWith("content:")) Uri.parse(mediaPath) else Uri.fromFile(File(mediaPath)) }
    val isImage = FileType.isImage(mediaPath)
    val isAudio = FileType.isAudio(mediaPath)
    val scope = rememberCoroutineScope()

    LaunchedEffect(mediaPath) {
        if (!isImage) {
            val position = if (state.preferences.resumePlayback) app.playbackPositions.get(mediaPath) else 0L
            engine.play(uri, position)
        }
    }
    DisposableEffect(mediaPath) {
        onDispose {
            if (!isImage) {
                scope.launch { app.playbackPositions.put(mediaPath, engine.exoPlayer.currentPosition) }
                if (!isAudio) engine.exoPlayer.pause()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(File(mediaPath).name) }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } })
        when {
            isImage -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AsyncImage(uri, mediaPath, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
            isVideo -> AndroidView({ PlayerView(it).apply { player = engine.exoPlayer; useController = true; keepScreenOn = true } }, Modifier.fillMaxSize())
            isAudio -> AudioControls(playback, File(mediaPath).name, engine)
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Unsupported media", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun AudioControls(playback: com.t3code.explorer.media.PlaybackState, title: String, engine: com.t3code.explorer.media.MediaEngine) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.PlayArrow, null, Modifier.size(120.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))
        Slider(value = if (playback.durationMs > 0) playback.positionMs.toFloat() / playback.durationMs else 0f, onValueChange = { fraction -> engine.exoPlayer.seekTo((fraction * playback.durationMs).toLong()) }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(formatDuration(playback.positionMs)); Text(formatDuration(playback.durationMs)) }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ engine.exoPlayer.seekToPrevious() }) { Icon(Icons.Default.SkipPrevious, null) }
            IconButton({ engine.toggle() }) { Icon(if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(52.dp)) }
            IconButton({ engine.exoPlayer.seekToNext() }) { Icon(Icons.Default.SkipNext, null) }
        }
    }
}
