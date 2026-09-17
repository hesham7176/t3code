package com.t3code.explorer.media

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.t3code.explorer.ExplorerApplication

/**
 * Foreground service that publishes the session for the shared player.
 *
 * Media3 turns the session into a notification with play/pause/next/previous controls, which is what
 * keeps audio playing (and controllable from the lock screen) while the UI is in the background.
 */
class ExplorerPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val application = application as ExplorerApplication
        session = application.playbackSessionHost.attach(application.mediaEngine.exoPlayer)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val engine = (application as? ExplorerApplication)?.mediaEngine
        if (engine == null || !engine.isPlaying || !engine.backgroundPlaybackEnabled) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        (application as? ExplorerApplication)?.playbackSessionHost?.detach()
        session = null
        super.onDestroy()
    }
}
