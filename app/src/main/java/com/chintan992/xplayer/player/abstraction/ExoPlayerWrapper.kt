package com.chintan992.xplayer.player.abstraction

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.chintan992.xplayer.HeaderStorage
import com.chintan992.xplayer.player.logic.TrackManager
import com.chintan992.xplayer.player.logic.GestureHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class ExoPlayerWrapper @Inject constructor(
    val exoPlayer: ExoPlayer,
    private val trackManager: TrackManager,
    private val headerStorage: HeaderStorage
) : UniversalPlayer {

    private val listeners = mutableListOf<UniversalPlayer.Listener>()
    private var decoderMode: DecoderMode = DecoderMode.AUTO

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                listeners.forEach { it.onIsPlayingChanged(isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val state = when (playbackState) {
                    Player.STATE_BUFFERING -> UniversalPlayer.STATE_BUFFERING
                    Player.STATE_READY -> UniversalPlayer.STATE_READY
                    Player.STATE_ENDED -> UniversalPlayer.STATE_ENDED
                    Player.STATE_IDLE -> UniversalPlayer.STATE_IDLE
                    else -> UniversalPlayer.STATE_IDLE
                }
                listeners.forEach { it.onPlaybackStateChanged(state) }
                
                if (playbackState == Player.STATE_READY) {
                    listeners.forEach { it.onDurationChanged(exoPlayer.duration.coerceAtLeast(0L)) }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val (audio, subtitle) = trackManager.getTracks(exoPlayer)
                listeners.forEach { it.onTracksChanged(audio, subtitle) }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                listeners.forEach { it.onDurationChanged(exoPlayer.duration.coerceAtLeast(0L)) }
                listeners.forEach { it.onPositionDiscontinuity(0L) }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                listeners.forEach { it.onPositionDiscontinuity(newPosition.positionMs) }
            }
            
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                listeners.forEach { it.onError(error.localizedMessage ?: "Unknown player error") }
            }
        })
    }

    override fun prepare(uri: Uri, title: String, subtitleUri: Uri?, headers: Map<String, String>) {
        if (headers.isNotEmpty()) {
            try {
                val host = java.net.URI(uri.toString()).host
                if (host != null) {
                    headerStorage.addHeaders(host, headers)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        val mediaItem = createMediaItem(uri, title, subtitleUri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    override fun play() {
        exoPlayer.play()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    override fun release() {
        exoPlayer.release()
    }

    override fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
    }
    
    override fun getPlaybackSpeed(): Float {
        return exoPlayer.playbackParameters.speed
    }

    override fun setVolume(volume: Float) {
        exoPlayer.volume = volume
    }

    override fun setDecoderMode(mode: DecoderMode) {
        this.decoderMode = mode
        val params = exoPlayer.trackSelectionParameters.buildUpon()
        
        when (mode) {
            DecoderMode.HARDWARE -> {
                params.setForceLowestBitrate(false)
            }
            DecoderMode.SOFTWARE -> {
                params.setForceLowestBitrate(true)
            }
            DecoderMode.AUTO -> {
                params.setForceLowestBitrate(false)
            }
            DecoderMode.HARDWARE_STRICT -> {
                params.setForceLowestBitrate(false)
            }
        }
        exoPlayer.trackSelectionParameters = params.build()
    }

    override fun getDuration(): Long = exoPlayer.duration.coerceAtLeast(0L)

    override fun getCurrentPosition(): Long = exoPlayer.currentPosition.coerceAtLeast(0L)

    override fun getBufferedPosition(): Long = exoPlayer.bufferedPosition.coerceAtLeast(0L)

    override fun isPlaying(): Boolean = exoPlayer.isPlaying

    override fun getTracks(): Pair<List<TrackInfo>, List<TrackInfo>> {
        return trackManager.getTracks(exoPlayer)
    }

    override fun selectAudioTrack(track: TrackInfo) {
        trackManager.selectAudioTrack(exoPlayer, track)
        notifyTracksChanged()
    }

    override fun selectSubtitleTrack(track: TrackInfo?) {
        trackManager.selectSubtitleTrack(exoPlayer, track)
        notifyTracksChanged()
    }
    
    private fun notifyTracksChanged() {
        val (audio, subtitle) = trackManager.getTracks(exoPlayer)
        listeners.forEach { it.onTracksChanged(audio, subtitle) }
    }

    override fun attachSubtitle(uri: Uri) {
         val currentMediaItem = exoPlayer.currentMediaItem
         if (currentMediaItem != null) {
             val videoUri = currentMediaItem.localConfiguration?.uri
             val videoTitle = currentMediaItem.mediaMetadata.title?.toString() ?: "Video"
             
             // Re-create MediaItem with new subtitle
             val position = exoPlayer.currentPosition
             val newMediaItem = createMediaItem(videoUri!!, videoTitle, uri)
             
             exoPlayer.setMediaItem(newMediaItem)
             exoPlayer.prepare()
             exoPlayer.seekTo(position)
             exoPlayer.play()
         }
    }

    override fun addListener(listener: UniversalPlayer.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: UniversalPlayer.Listener) {
        listeners.remove(listener)
    }

    private fun createMediaItem(uri: Uri, title: String, subtitleUri: Uri?): MediaItem {
        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setDisplayTitle(title)
            .setArtist("XPlayer")
            .build()

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(metadata)
            .setTag(title)
        
        if (subtitleUri != null) {
            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                .apply {
                    val path = subtitleUri.path
                    if (path != null) {
                        if (path.endsWith(".ass", ignoreCase = true) || path.endsWith(".ssa", ignoreCase = true)) {
                            setMimeType(MimeTypes.TEXT_SSA)
                        } else if (path.endsWith(".vtt", ignoreCase = true)) {
                            setMimeType(MimeTypes.TEXT_VTT)
                        } else {
                            setMimeType(MimeTypes.APPLICATION_SUBRIP)
                        }
                    } else {
                         setMimeType(MimeTypes.APPLICATION_SUBRIP)
                    }
                    setLanguage("und")
                    setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                }
                .build()
            mediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
        }
        
        return mediaItemBuilder.build()
    }
}
