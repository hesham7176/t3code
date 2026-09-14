package com.t3code.explorer.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Media3 engine kept independent from the UI layer. */
class MediaEngine(context: Context) : Player.Listener {
    private val player = ExoPlayer.Builder(context).build()
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()
    val exoPlayer: ExoPlayer get() = player

    init {
        player.addListener(this)
    }

    fun setQueue(uris: List<Uri>, startIndex: Int = 0, startPosition: Long = 0L) {
        player.setMediaItems(uris.map(MediaItem::fromUri), startIndex, startPosition)
        player.prepare()
    }

    fun play(uri: Uri, startPosition: Long = 0L) {
        player.setMediaItem(MediaItem.fromUri(uri), startPosition)
        player.prepare()
        player.play()
    }

    fun toggle() { if (player.isPlaying) player.pause() else player.play() }
    fun seekBy(deltaMs: Long) { player.seekTo((player.currentPosition + deltaMs).coerceAtLeast(0L)) }
    fun release() { player.release() }

    override fun onIsPlayingChanged(isPlaying: Boolean) { publish() }
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { publish() }
    override fun onPlaybackStateChanged(playbackState: Int) { publish() }

    private fun publish() {
        _state.value = PlaybackState(player.isPlaying, player.currentPosition, player.duration.coerceAtLeast(0), player.currentMediaItem?.mediaId.orEmpty())
    }
}

data class PlaybackState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val mediaId: String = ""
)
