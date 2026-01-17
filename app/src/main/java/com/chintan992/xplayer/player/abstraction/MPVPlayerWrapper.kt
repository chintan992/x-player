package com.chintan992.xplayer.player.abstraction

import android.content.Context
import android.net.Uri
import android.util.Log
import `is`.xyz.mpv.MPVLib
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MPVPlayerWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) : UniversalPlayer {

    private val listeners = mutableListOf<UniversalPlayer.Listener>()
    private var isPrepared = false
    private var playbackSpeed = 1.0f
    private var volume = 1.0f

    companion object {
        const val MPV_FORMAT_STRING = 1
        const val MPV_FORMAT_FLAG = 3
        const val MPV_FORMAT_INT64 = 4
        const val MPV_FORMAT_DOUBLE = 5
        const val MPV_EVENT_END_FILE = 7
    }

    private val mpvObserver = object : MPVLib.EventObserver {
        override fun eventProperty(property: String, value: Long) {
             when (property) {
                "time-pos" -> {
                     listeners.forEach { it.onPositionDiscontinuity(value * 1000) }
                }
                "duration" -> {
                    listeners.forEach { it.onDurationChanged(value * 1000) }
                }
             }
        }

        override fun eventProperty(property: String, value: Boolean) {
             if (property == "pause") {
                 listeners.forEach { it.onIsPlayingChanged(!value) }
             }
        }

        override fun eventProperty(property: String, value: String) {
            if (property == "media-title" || property == "filename") {
                 listeners.forEach { it.onMediaMetadataChanged(value) }
            }
        }
        
        // Add missing overload if any (Double)
        override fun eventProperty(property: String, value: Double) {
        }

        override fun eventProperty(property: String) {
        }
        
        override fun eventProperty(property: String, value: `is`.xyz.mpv.MPVNode) {
        }
        
        override fun event(eventId: Int) {
            if (eventId == MPV_EVENT_END_FILE) {
                listeners.forEach { it.onPlaybackStateChanged(UniversalPlayer.STATE_ENDED) }
            }
        }
    }

    // Hold reference to surface to re-attach on re-init
    private var currentSurface: android.view.Surface? = null

    init {
        // Initialize MPVLib globally here
        MPVLib.create(context)
        MPVLib.setOptionString("vo", "gpu")
        MPVLib.setOptionString("gpu-context", "android")
        MPVLib.setOptionString("hwdec", "auto")
        MPVLib.setOptionString("keep-open", "yes") // Don't terminate on end of file
        MPVLib.setOptionString("tls-verify", "no")
        MPVLib.setOptionString("network-timeout", "10")
        MPVLib.init()

        MPVLib.addObserver(mpvObserver)
        MPVLib.observeProperty("time-pos", MPV_FORMAT_INT64)
        MPVLib.observeProperty("duration", MPV_FORMAT_INT64)
        MPVLib.observeProperty("pause", MPV_FORMAT_FLAG)
        MPVLib.observeProperty("media-title", MPV_FORMAT_STRING)
        MPVLib.observeProperty("filename", MPV_FORMAT_STRING)
        
        // Re-attach surface if we have one (unlikely on first init, but good for safety)
        currentSurface?.let { MPVLib.attachSurface(it) }
    }

    override fun prepare(uri: Uri, title: String, subtitleUri: Uri?, headers: Map<String, String>) {
         // Headers: MPVLib.setOptionString("http-header-fields", "Header: Value, ...")
         
         // Fix: Javap confirmed signature is `command(String...)`.
         // In Kotlin we can pass arguments directly.
         // MPV 'loadfile' takes the filename (URI) as the second argument.
         MPVLib.command("loadfile", uri.toString())
         
         if (subtitleUri != null) {
             MPVLib.command("sub-add", subtitleUri.toString())
         }
         
         isPrepared = true
         listeners.forEach { it.onPlaybackStateChanged(UniversalPlayer.STATE_READY) }
    }

    override fun play() {
        MPVLib.setPropertyBoolean("pause", false)
    }

    override fun pause() {
        MPVLib.setPropertyBoolean("pause", true)
    }

    override fun seekTo(position: Long) {
        val posSeconds = position / 1000.0
        MPVLib.setPropertyDouble("time-pos", posSeconds)
    }

    override fun release() {
        MPVLib.removeObserver(mpvObserver)
        MPVLib.command("stop")
        MPVLib.destroy()
    }

    override fun setPlaybackSpeed(speed: Float) {
        this.playbackSpeed = speed
        MPVLib.setPropertyDouble("speed", speed.toDouble())
    }

    override fun setVolume(volume: Float) {
        this.volume = volume
        MPVLib.setPropertyInt("volume", (volume * 100).toInt())
    }

    override fun setDecoderMode(mode: DecoderMode) {
        val hwdec = when (mode) {
            DecoderMode.HARDWARE -> "mediacodec"
            DecoderMode.HARDWARE_STRICT -> "mediacodec-copy"
            DecoderMode.SOFTWARE -> "no"
            DecoderMode.AUTO -> "auto"
        }
        MPVLib.setPropertyString("hwdec", hwdec)
    }

    override fun setSeekParameters(exact: Boolean) {
        MPVLib.setPropertyString("hr-seek", if (exact) "yes" else "no")
    }

    override fun getDuration(): Long {
        val duration = MPVLib.getPropertyDouble("duration") ?: 0.0
        return (duration * 1000).toLong()
    }

    override fun getCurrentPosition(): Long {
        val pos = MPVLib.getPropertyDouble("time-pos") ?: 0.0
        return (pos * 1000).toLong()
    }

    override fun getBufferedPosition(): Long {
        return getCurrentPosition() 
    }

    override fun isPlaying(): Boolean {
        // !! or ?: false
        return !(MPVLib.getPropertyBoolean("pause") ?: true)
    }

    override fun getPlaybackSpeed(): Float {
        return (MPVLib.getPropertyDouble("speed") ?: 1.0).toFloat()
    }

    override fun getTracks(): Pair<List<TrackInfo>, List<TrackInfo>> {
        val count = MPVLib.getPropertyInt("track-list/count") ?: 0
        val audioTracks = mutableListOf<TrackInfo>()
        val subTracks = mutableListOf<TrackInfo>()
        
        for (i in 0 until count) {
            val type = MPVLib.getPropertyString("track-list/$i/type") ?: ""
            val id = MPVLib.getPropertyInt("track-list/$i/id") ?: 0
            val lang = MPVLib.getPropertyString("track-list/$i/lang")
            val title = MPVLib.getPropertyString("track-list/$i/title") ?: ""
            val selected = MPVLib.getPropertyBoolean("track-list/$i/selected") ?: false
            
            val trackInfo = TrackInfo(
                index = id,
                groupIndex = 0,
                name = title,
                language = lang,
                isSelected = selected,
                isSupported = true
            )
            
            if (type == "audio") {
                audioTracks.add(trackInfo)
            } else if (type == "sub") {
                subTracks.add(trackInfo)
            }
        }
        return Pair(audioTracks, subTracks)
    }

    override fun selectAudioTrack(track: TrackInfo) {
        MPVLib.setPropertyString("aid", track.index.toString())
    }

    override fun selectSubtitleTrack(track: TrackInfo?) {
         if (track == null) {
             MPVLib.setPropertyString("sid", "no")
         } else {
             MPVLib.setPropertyString("sid", track.index.toString())
         }
    }

    override fun attachSubtitle(uri: Uri) {
        MPVLib.command("sub-add", uri.toString())
    }

    // Called by MPVAndroidView
    fun attachSurface(surface: android.view.Surface) {
        currentSurface = surface
        MPVLib.attachSurface(surface)
    }

    fun detachSurface() {
        currentSurface = null
        MPVLib.detachSurface()
    }

    override fun addListener(listener: UniversalPlayer.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: UniversalPlayer.Listener) {
        listeners.remove(listener)
    }
}


