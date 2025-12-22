package com.chintan992.xplayer

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService(), com.google.android.gms.wearable.MessageClient.OnMessageReceivedListener {
    @Inject
    lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession.Builder(this, player).build()
        com.google.android.gms.wearable.Wearable.getMessageClient(this).addListener(this)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        com.google.android.gms.wearable.Wearable.getMessageClient(this).removeListener(this)
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onMessageReceived(messageEvent: com.google.android.gms.wearable.MessageEvent) {
        val path = messageEvent.path
        // Ensure commands are executed on the main thread
        android.os.Handler(android.os.Looper.getMainLooper()).post {
             when (path) {
                "/control/play" -> player.play()
                "/control/pause" -> player.pause()
                "/control/rewind" -> player.seekBack()
                "/control/ffwd" -> player.seekForward()
             }
        }
    }
}
