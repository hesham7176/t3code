package com.t3code.explorer.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.t3code.explorer.domain.util.MediaQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Media3 engine kept independent from the UI layer.
 *
 * The player is a process wide singleton (owned by the application) so the [ExplorerPlaybackService]
 * can publish a MediaSession for the very same player: notification and lock-screen controls then
 * drive the playback the user actually hears.
 */
class MediaEngine(context: Context) : Player.Listener {
    private val appContext = context.applicationContext
    private val player = ExoPlayer.Builder(appContext)
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ticker: Job
    private val _state = MutableStateFlow(PlaybackState())
    private var lastError: String? = null
    private var queue = MediaQueue<Uri>()

    /** When true, playback continues with a foreground service notification after leaving the app. */
    var backgroundPlaybackEnabled: Boolean = true

    val state: StateFlow<PlaybackState> = _state.asStateFlow()
    val exoPlayer: ExoPlayer get() = player
    val isPlaying: Boolean get() = player.isPlaying
    val queueSize: Int get() = queue.size

    init {
        player.addListener(this)
        ticker = scope.launch {
            while (isActive) {
                publish()
                delay(250)
            }
        }
    }

    fun setQueue(uris: List<Uri>, startIndex: Int = 0, startPosition: Long = 0L) {
        lastError = null
        queue = MediaQueue(uris, startIndex)
        if (uris.isEmpty()) {
            player.clearMediaItems()
            publish()
            return
        }
        startPlaybackService()
        player.setMediaItems(uris.map(::mediaItem), startIndex.coerceIn(0, uris.lastIndex), startPosition.coerceAtLeast(0L))
        player.prepare()
        player.play()
        publish()
    }

    fun play(uri: Uri, startPosition: Long = 0L) {
        lastError = null
        queue = MediaQueue(listOf(uri), 0)
        startPlaybackService()
        player.setMediaItem(mediaItem(uri), startPosition)
        player.prepare()
        player.play()
        publish()
    }

    fun toggle() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.currentMediaItem == null) return
            startPlaybackService()
            player.play()
        }
    }

    fun next(): Boolean {
        val target = queue.nextIndex() ?: return false
        queue.moveTo(target)
        player.seekTo(target, 0L)
        return true
    }

    fun previous(): Boolean {
        val target = queue.previousIndex() ?: return false
        queue.moveTo(target)
        player.seekTo(target, 0L)
        return true
    }

    fun setRepeatMode(mode: MediaQueue.RepeatMode) {
        queue.repeatMode = mode
        player.repeatMode = when (mode) {
            MediaQueue.RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            MediaQueue.RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            MediaQueue.RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        publish()
    }

    fun setShuffle(enabled: Boolean) {
        queue.shuffleEnabled = enabled
        player.shuffleModeEnabled = enabled
        publish()
    }

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed.coerceIn(0.25f, 3f))
        publish()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        publish()
    }

    fun seekBy(deltaMs: Long) {
        seekTo(player.currentPosition + deltaMs)
    }

    /** Recovers from a playback error by preparing the current item again. */
    fun retry() {
        lastError = null
        if (player.currentMediaItem != null) {
            player.prepare()
            player.play()
        }
        publish()
    }

    fun release() {
        ticker.cancel()
        scope.cancel()
        stopPlaybackService()
        player.release()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) { publish() }
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (player.currentMediaItemIndex >= 0) queue.moveTo(player.currentMediaItemIndex)
        publish()
    }
    override fun onPlaybackStateChanged(playbackState: Int) { publish() }
    override fun onPlayerError(error: PlaybackException) {
        lastError = error.message ?: error.errorCodeName
        stopPlaybackService()
        publish()
    }

    private fun startPlaybackService() {
        if (!backgroundPlaybackEnabled) return
        runCatching { appContext.startForegroundService(Intent(appContext, ExplorerPlaybackService::class.java)) }
    }

    private fun stopPlaybackService() {
        runCatching { appContext.stopService(Intent(appContext, ExplorerPlaybackService::class.java)) }
    }

    private fun mediaItem(uri: Uri): MediaItem {
        val title = uri.lastPathSegment
            ?.substringAfterLast(':')
            ?.substringAfterLast('/')
            .orEmpty()
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(uri.toString())
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title.ifBlank { uri.toString() }).build())
            .build()
    }

    private fun publish() {
        _state.value = PlaybackState(
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition,
            durationMs = player.duration.coerceAtLeast(0),
            mediaId = player.currentMediaItem?.localConfiguration?.uri?.toString().orEmpty(),
            errorMessage = lastError,
            speed = player.playbackParameters.speed,
            index = queue.currentIndex,
            queueSize = queue.size,
            hasNext = queue.hasNext(),
            hasPrevious = queue.hasPrevious()
        )
    }
}

data class PlaybackState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val mediaId: String = "",
    val errorMessage: String? = null,
    val speed: Float = 1f,
    val index: Int = -1,
    val queueSize: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
)
