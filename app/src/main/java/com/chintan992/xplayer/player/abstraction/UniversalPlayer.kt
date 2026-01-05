package com.chintan992.xplayer.player.abstraction

import android.net.Uri

interface UniversalPlayer {
    
    fun prepare(uri: Uri, title: String, subtitleUri: Uri?, headers: Map<String, String> = emptyMap())
    fun play()
    fun pause()
    fun seekTo(position: Long)
    fun release()
    
    fun setPlaybackSpeed(speed: Float)
    fun setVolume(volume: Float)
    fun setDecoderMode(mode: DecoderMode)
    
    fun getDuration(): Long
    fun getCurrentPosition(): Long
    fun getBufferedPosition(): Long
    fun isPlaying(): Boolean
    fun getPlaybackSpeed(): Float
    
    fun getTracks(): Pair<List<TrackInfo>, List<TrackInfo>>
    fun selectAudioTrack(track: TrackInfo)
    fun selectSubtitleTrack(track: TrackInfo?)
    
    /**
     * Replaces the current media item with a new one that includes the subtitle, 
     * restoring the playback position.
     * This is an abstraction of the specific "reload with subtitle" logic.
     */
    fun attachSubtitle(uri: Uri)

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)

    interface Listener {
        fun onIsPlayingChanged(isPlaying: Boolean)
        fun onPlaybackStateChanged(state: Int)
        fun onDurationChanged(duration: Long)
        fun onPositionDiscontinuity(currentPosition: Long)
        fun onTracksChanged(audioTracks: List<TrackInfo>, subtitleTracks: List<TrackInfo>)
        fun onMediaMetadataChanged(title: String?)
        fun onError(error: String)
    }

    companion object {
        const val STATE_IDLE = 1
        const val STATE_BUFFERING = 2
        const val STATE_READY = 3
        const val STATE_ENDED = 4
    }
}
