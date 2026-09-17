package com.t3code.explorer.media

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.t3code.explorer.MainActivity

/**
 * Owns the MediaSession attached to the shared ExoPlayer.
 *
 * The session is created by the foreground service and released with it; the player itself belongs
 * to [MediaEngine] and is never released here, so stopping the service does not stop playback.
 */
class PlaybackSessionHost(private val context: Context) {
    private var session: MediaSession? = null
    private var attachedPlayer: ExoPlayer? = null

    @Synchronized
    fun attach(player: ExoPlayer): MediaSession {
        val existing = session
        if (existing != null && attachedPlayer === player) return existing
        existing?.release()
        val launchIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val created = MediaSession.Builder(context, player).setSessionActivity(launchIntent).build()
        session = created
        attachedPlayer = player
        return created
    }

    @Synchronized
    fun detach() {
        session?.release()
        session = null
        attachedPlayer = null
    }
}
