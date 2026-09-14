package com.t3code.explorer.ui.screens

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.t3code.explorer.ExplorerApplication
import com.t3code.explorer.R
import com.t3code.explorer.domain.util.FileType
import com.t3code.explorer.domain.util.GestureAction
import com.t3code.explorer.domain.util.VideoGestureDecider
import com.t3code.explorer.ui.ExplorerUiState
import com.t3code.explorer.ui.util.formatDuration
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
            isVideo -> Box(Modifier.fillMaxSize()) {
                AndroidView({ PlayerView(it).apply { player = engine.exoPlayer; useController = true; keepScreenOn = true } }, Modifier.fillMaxSize())
                VideoGestureOverlay(context, engine)
            }
            isAudio -> AudioControls(playback, File(mediaPath).name, engine)
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.unsupported_file), color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun VideoGestureOverlay(context: Context, engine: com.t3code.explorer.media.MediaEngine) {
    val decider = remember { VideoGestureDecider() }
    var leftSide by remember { mutableStateOf(false) }
    var lockedAction by remember { mutableStateOf(GestureAction.NONE) }
    Box(
        Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { engine.toggle() },
                    onDoubleTap = { offset -> engine.seekBy(if (offset.x < size.width / 2f) -10_000L else 10_000L) }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> leftSide = offset.x < size.width / 2f; lockedAction = GestureAction.NONE },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (lockedAction == GestureAction.NONE) lockedAction = decider.decide(dragAmount.x, dragAmount.y, size.width.toFloat(), leftSide).action
                        when (lockedAction) {
                            GestureAction.SEEK -> engine.seekBy((dragAmount.x / size.width * 30_000f).toLong())
                            GestureAction.VOLUME -> changeVolume(context, dragAmount.y)
                            GestureAction.BRIGHTNESS -> changeBrightness(context, dragAmount.y)
                            else -> Unit
                        }
                    },
                    onDragEnd = { lockedAction = GestureAction.NONE }
                )
            }
    )
}

private fun changeVolume(context: Context, deltaY: Float) {
    val audio = context.getSystemService(AudioManager::class.java) ?: return
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
    audio.setStreamVolume(AudioManager.STREAM_MUSIC, (current - deltaY / 80f).toInt().coerceIn(0, max), 0)
}

private fun changeBrightness(context: Context, deltaY: Float) {
    val activity = context as? Activity ?: return
    val attributes = activity.window.attributes
    val current = if (attributes.screenBrightness < 0f) 0.5f else attributes.screenBrightness
    attributes.screenBrightness = (current - deltaY / 800f).coerceIn(0.01f, 1f)
    activity.window.attributes = attributes
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
