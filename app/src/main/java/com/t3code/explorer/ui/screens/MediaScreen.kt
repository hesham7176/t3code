package com.t3code.explorer.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.view.ViewConfiguration
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.t3code.explorer.ExplorerApplication
import com.t3code.explorer.R
import com.t3code.explorer.domain.model.FileItem
import com.t3code.explorer.domain.util.FileType
import com.t3code.explorer.domain.util.GestureAction
import com.t3code.explorer.domain.util.GestureConfig
import com.t3code.explorer.domain.util.GestureUpdate
import com.t3code.explorer.domain.util.VideoGestureController
import com.t3code.explorer.media.MediaEngine
import com.t3code.explorer.media.MediaMetadataReader
import com.t3code.explorer.media.PlaybackState
import com.t3code.explorer.ui.ExplorerUiState
import com.t3code.explorer.ui.ExplorerViewModel
import com.t3code.explorer.ui.util.formatDuration
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PLAYBACK_SPEEDS = listOf(0.5f, 1f, 1.25f, 1.5f, 2f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(
    state: ExplorerUiState,
    mediaPath: String,
    isVideo: Boolean,
    mediaMimeType: String = "",
    viewModel: ExplorerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as ExplorerApplication
    val engine = app.mediaEngine
    val playback by engine.state.collectAsStateWithLifecycle()
    val uri = remember(mediaPath) { if (mediaPath.startsWith("content:")) Uri.parse(mediaPath) else Uri.fromFile(File(mediaPath)) }
    val displayName = remember(mediaPath) { uri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/') ?: File(mediaPath).name }
    val isImage = mediaMimeType.startsWith("image/") || FileType.isImage(mediaPath)
    val isAudio = mediaMimeType.startsWith("audio/") || FileType.isAudio(mediaPath)
    var fullscreen by rememberSaveable(mediaPath) { mutableStateOf(false) }
    val activity = context as? Activity
    val originalOrientation = remember(activity) { activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    val queue = remember(state.items) {
        state.items.filter { !it.isDirectory && (it.mimeType.startsWith("video/") || it.mimeType.startsWith("audio/")) }
    }
    val scope = rememberCoroutineScope()

    DisposableEffect(fullscreen, isVideo) {
        activity?.let {
            WindowCompat.setDecorFitsSystemWindows(it.window, !fullscreen)
            val controller = WindowInsetsControllerCompat(it.window, it.window.decorView)
            if (fullscreen) controller.hide(WindowInsetsCompat.Type.systemBars()) else controller.show(WindowInsetsCompat.Type.systemBars())
            if (isVideo) it.requestedOrientation = if (fullscreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else originalOrientation
        }
        onDispose {
            activity?.let {
                WindowCompat.setDecorFitsSystemWindows(it.window, true)
                WindowInsetsControllerCompat(it.window, it.window.decorView).show(WindowInsetsCompat.Type.systemBars())
                if (isVideo) it.requestedOrientation = originalOrientation
            }
        }
    }

    LaunchedEffect(mediaPath, queue) {
        if (!isImage) {
            val position = if (state.preferences.resumePlayback) app.playbackPositions.get(mediaPath) else 0L
            val index = queue.indexOfFirst { it.path == mediaPath }
            if (queue.size > 1 && index >= 0) engine.setQueue(queue.map(::mediaUri), index, position)
            else engine.play(uri, position)
        }
    }
    DisposableEffect(mediaPath) {
        onDispose {
            if (!isImage) {
                scope.launch {
                    val position = engine.exoPlayer.currentPosition
                    app.playbackPositions.put(mediaPath, position)
                    app.playbackPositions.trim(mediaPath, engine.exoPlayer.duration)
                }
                if (!isAudio) engine.exoPlayer.pause()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(displayName, maxLines = 1) },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            actions = {
                if (!isImage) {
                    IconButton(enabled = playback.hasPrevious, onClick = { engine.previous() }) { Icon(Icons.Default.SkipPrevious, stringResource(R.string.previous)) }
                    IconButton(enabled = playback.hasNext, onClick = { engine.next() }) { Icon(Icons.Default.SkipNext, stringResource(R.string.next)) }
                    TextButton(onClick = { engine.setSpeed(nextSpeed(playback.speed)) }) { Text("${playback.speed}x") }
                    if (isVideo) IconButton(onClick = { fullscreen = !fullscreen }) { Icon(Icons.Default.Fullscreen, stringResource(R.string.full_screen)) }
                }
            }
        )
        when {
            isImage -> ImageGallery(state, mediaPath, viewModel)
            isVideo -> VideoPlayer(context, engine, playback, Modifier.fillMaxSize())
            isAudio -> AudioControls(engine, playback, mediaPath, displayName, Modifier.fillMaxSize())
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.unsupported_file), color = MaterialTheme.colorScheme.error) }
        }
    }
}

private fun mediaUri(item: FileItem): Uri =
    if (item.path.startsWith("content:")) Uri.parse(item.path) else Uri.fromFile(File(item.path))

private fun nextSpeed(current: Float): Float {
    val index = PLAYBACK_SPEEDS.indexOfFirst { kotlin.math.abs(it - current) < 0.01f }
    return PLAYBACK_SPEEDS[(index + 1).mod(PLAYBACK_SPEEDS.size)]
}

@Composable
private fun VideoPlayer(context: Context, engine: MediaEngine, playback: PlaybackState, modifier: Modifier = Modifier) {
    Column(modifier) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(
                factory = { PlayerView(it).apply { player = engine.exoPlayer; useController = false; keepScreenOn = true } },
                modifier = Modifier.fillMaxSize()
            )
            VideoGestureLayer(context, engine, Modifier.fillMaxSize())
            playback.errorMessage?.let { message ->
                PlaybackErrorCard(message, onRetry = engine::retry, modifier = Modifier.align(Alignment.TopCenter).padding(12.dp))
            }
        }
        VideoControls(engine, playback, Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

@Composable
private fun VideoControls(engine: MediaEngine, playback: PlaybackState, modifier: Modifier = Modifier) {
    val duration = playback.durationMs
    Column(modifier) {
        Slider(
            value = if (duration > 0) (playback.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f,
            onValueChange = { fraction -> engine.seekTo((fraction * duration).toLong()) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(formatDuration(playback.positionMs), style = MaterialTheme.typography.labelMedium)
            Text(" / ", style = MaterialTheme.typography.labelMedium)
            Text(formatDuration(duration), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            IconButton(enabled = playback.hasPrevious, onClick = { engine.previous() }) { Icon(Icons.Default.SkipPrevious, stringResource(R.string.previous)) }
            IconButton(onClick = { engine.toggle() }) {
                Icon(if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (playback.isPlaying) stringResource(R.string.pause) else stringResource(R.string.play))
            }
            IconButton(enabled = playback.hasNext, onClick = { engine.next() }) { Icon(Icons.Default.SkipNext, stringResource(R.string.next)) }
        }
    }
}

private data class GestureIndicator(val action: GestureAction, val text: String, val progress: Float)

@Composable
private fun VideoGestureLayer(context: Context, engine: MediaEngine, modifier: Modifier = Modifier) {
    val activity = context as? Activity
    val audio = remember(context) { context.getSystemService(AudioManager::class.java) }
    val touchSlop = remember(context) { ViewConfiguration.get(context).scaledTouchSlop.toFloat() }
    val controller = remember(touchSlop) { VideoGestureController(GestureConfig(touchSlopPx = touchSlop)) }
    var indicator by remember { mutableStateOf<GestureIndicator?>(null) }
    var seekBase by remember { mutableStateOf(0L) }
    var volumeBase by remember { mutableStateOf(0) }
    var volumeMax by remember { mutableStateOf(0) }
    var brightnessBase by remember { mutableStateOf(0.5f) }

    fun apply(update: GestureUpdate) {
        if (update.justLocked) {
            seekBase = engine.exoPlayer.currentPosition
            volumeMax = audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
            volumeBase = audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
            brightnessBase = activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f
        }
        when (update.action) {
            GestureAction.SEEK -> {
                val duration = engine.exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                val target = (seekBase + controller.seekDeltaMs(update.fraction)).coerceIn(0L, duration)
                engine.seekTo(target)
                val offset = target - seekBase
                indicator = GestureIndicator(GestureAction.SEEK, "${formatDuration(target)}  ${signedSeconds(offset)}", update.fraction)
            }
            GestureAction.VOLUME -> {
                val max = volumeMax
                if (audio != null && max > 0) {
                    val value = (volumeBase + update.fraction * max).roundToInt().coerceIn(0, max)
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
                    indicator = GestureIndicator(GestureAction.VOLUME, "${context.getString(R.string.volume)} $value/$max", value.toFloat() / max)
                }
            }
            GestureAction.BRIGHTNESS -> {
                val target = activity ?: return
                val value = (brightnessBase + update.fraction).coerceIn(0.01f, 1f)
                target.window.attributes = target.window.attributes.apply { screenBrightness = value }
                indicator = GestureIndicator(GestureAction.BRIGHTNESS, "${context.getString(R.string.brightness)} ${(value * 100).roundToInt()}%", value)
            }
            else -> return
        }
    }

    Box(
        modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { engine.toggle() },
                    onDoubleTap = { offset ->
                        when (controller.doubleTapAction(offset.x, size.width.toFloat())) {
                            GestureAction.DOUBLE_TAP_FORWARD -> engine.seekBy(DOUBLE_TAP_SEEK_MS)
                            GestureAction.DOUBLE_TAP_BACKWARD -> engine.seekBy(-DOUBLE_TAP_SEEK_MS)
                            else -> engine.toggle()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> controller.onDragStart(offset.x, size.width.toFloat(), size.height.toFloat()) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        controller.onDrag(dragAmount.x, dragAmount.y)?.let { update -> apply(update) }
                    },
                    onDragEnd = { indicator = null },
                    onDragCancel = { indicator = null }
                )
            }
    ) {
        indicator?.let { current -> GestureIndicatorCard(current, Modifier.align(Alignment.Center)) }
    }
}

@Composable
private fun GestureIndicatorCard(indicator: GestureIndicator, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f)) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(indicator.text, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator({ indicator.progress.coerceIn(0f, 1f) }, Modifier.width(140.dp))
        }
    }
}

@Composable
private fun PlaybackErrorCard(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            Spacer(Modifier.width(8.dp))
            TextButton(onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun AudioControls(
    engine: MediaEngine,
    playback: PlaybackState,
    path: String,
    title: String,
    modifier: Modifier = Modifier
) {
    var artwork by remember(path) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(path) {
        artwork = if (path.startsWith("content:")) null else withContext(Dispatchers.IO) { MediaMetadataReader().artwork(File(path)) }
    }
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        val bitmap = artwork
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), title, Modifier.size(180.dp), contentScale = ContentScale.Crop)
        } else {
            Surface(Modifier.size(180.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, null, Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary) }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        playback.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center) }
        Spacer(Modifier.height(16.dp))
        val duration = playback.durationMs
        Slider(
            value = if (duration > 0) (playback.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f,
            onValueChange = { fraction -> engine.seekTo((fraction * duration).toLong()) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(playback.positionMs), style = MaterialTheme.typography.labelMedium)
            Text(formatDuration(duration), style = MaterialTheme.typography.labelMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = playback.hasPrevious, onClick = { engine.previous() }) { Icon(Icons.Default.SkipPrevious, stringResource(R.string.previous)) }
            IconButton(onClick = { engine.toggle() }) {
                Icon(if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(52.dp))
            }
            IconButton(enabled = playback.hasNext, onClick = { engine.next() }) { Icon(Icons.Default.SkipNext, stringResource(R.string.next)) }
        }
    }
}

@Composable
private fun ImageGallery(state: ExplorerUiState, mediaPath: String, viewModel: ExplorerViewModel) {
    val images = remember(state.items, mediaPath) {
        state.items.filter { !it.isDirectory && it.mimeType.startsWith("image/") }.map { it.path }.ifEmpty { listOf(mediaPath) }
    }
    val initialPage = remember(mediaPath, images) { images.indexOf(mediaPath).coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { images.size })
    var propertiesTarget by remember { mutableStateOf<FileItem?>(null) }
    Column(Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
            val path = images[page]
            ZoomableImage(if (path.startsWith("content:")) Uri.parse(path) else Uri.fromFile(File(path)), path)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${pagerState.currentPage + 1} / ${images.size}", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.weight(1f))
            val current = state.items.firstOrNull { it.path == images[pagerState.currentPage] }
            IconButton(enabled = current != null, onClick = { current?.let(viewModel::share) }) { Icon(Icons.Default.Share, stringResource(R.string.share)) }
            IconButton(enabled = current != null, onClick = { current?.let { propertiesTarget = it } }) { Icon(Icons.Default.Info, stringResource(R.string.properties)) }
            IconButton(enabled = current != null, onClick = { current?.let(viewModel::deleteItem) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
        }
    }
    propertiesTarget?.let { item -> PropertiesDialog(item) { propertiesTarget = null } }
}

@Composable
private fun ZoomableImage(uri: Uri, description: String) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val maxX = with(density) { (maxWidth.toPx() * (scale - 1f)) / 2f }
        val maxY = with(density) { (maxHeight.toPx() * (scale - 1f)) / 2f }
        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, 5f)
            offset = Offset((offset.x + panChange.x).coerceIn(-maxX, maxX), (offset.y + panChange.y).coerceIn(-maxY, maxY))
        }
        Box(
            Modifier
                .fillMaxSize()
                .transformable(transformState)
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        scale = if (scale > 1.5f) 1f else 2.5f
                        offset = Offset.Zero
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = uri,
                contentDescription = description,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

private fun signedSeconds(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    return if (seconds >= 0) "+$seconds s" else "$seconds s"
}

private const val DOUBLE_TAP_SEEK_MS = 10_000L
