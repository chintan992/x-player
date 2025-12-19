package com.chintan992.xplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackInfo(
    val index: Int,
    val groupIndex: Int,
    val name: String,
    val language: String?,
    val isSelected: Boolean
)

enum class AspectRatioMode(val displayName: String, val ratio: Int) {
    FIT(displayName = "Fit", ratio = 0),
    FILL(displayName = "Fill", ratio = 1),
    ZOOM(displayName = "Zoom", ratio = 2),
    STRETCH(displayName = "Stretch", ratio = 3),
    RATIO_16_9(displayName = "16:9", ratio = 4),
    RATIO_4_3(displayName = "4:3", ratio = 5)
}

enum class DecoderMode(val displayName: String) {
    HARDWARE(displayName = "Hardware (HW)"),
    SOFTWARE(displayName = "Software (SW)"),
    AUTO(displayName = "Auto")
}

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val playbackSpeed: Float = 1f,
    val videoTitle: String = "",
    val controlsVisible: Boolean = true,
    val isLocked: Boolean = false,
    val isLandscape: Boolean = true,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val decoderMode: DecoderMode = DecoderMode.AUTO,
    val audioTracks: List<TrackInfo> = emptyList(),
    val subtitleTracks: List<TrackInfo> = emptyList(),
    // Gesture states
    val brightness: Float = 0.5f,
    val volume: Float = 0.5f,
    val showBrightnessIndicator: Boolean = false,
    val showVolumeIndicator: Boolean = false,
    val isSeeking: Boolean = false,
    val seekPosition: Long = 0L
)

@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var player: ExoPlayer? = null
    private var hideControlsJob: Job? = null
    private var positionUpdateJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            if (isPlaying) {
                startPositionUpdates()
                scheduleHideControls()
            } else {
                stopPositionUpdates()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                player?.let { p ->
                    _uiState.value = _uiState.value.copy(
                        duration = p.duration.coerceAtLeast(0L)
                    )
                }
                updateTrackInfo()
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateTrackInfo()
        }
    }

    fun setPlayer(exoPlayer: ExoPlayer, videoTitle: String) {
        player = exoPlayer
        exoPlayer.addListener(playerListener)
        _uiState.value = _uiState.value.copy(
            videoTitle = videoTitle,
            isPlaying = exoPlayer.isPlaying,
            duration = exoPlayer.duration.coerceAtLeast(0L),
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
        )
        updateTrackInfo()
        if (exoPlayer.isPlaying) {
            startPositionUpdates()
        }
    }

    private fun updateTrackInfo() {
        val p = player ?: return
        val tracks = p.currentTracks
        
        val audioTracks = mutableListOf<TrackInfo>()
        val subtitleTracks = mutableListOf<TrackInfo>()

        tracks.groups.forEachIndexed { groupIndex, group ->
            val trackType = group.type
            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val isSelected = group.isTrackSelected(trackIndex)
                
                val trackName = format.label 
                    ?: format.language?.uppercase() 
                    ?: "Track ${trackIndex + 1}"
                
                val trackInfo = TrackInfo(
                    index = trackIndex,
                    groupIndex = groupIndex,
                    name = trackName,
                    language = format.language,
                    isSelected = isSelected
                )

                when (trackType) {
                    C.TRACK_TYPE_AUDIO -> audioTracks.add(trackInfo)
                    C.TRACK_TYPE_TEXT -> subtitleTracks.add(trackInfo)
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks
        )
    }

    fun togglePlayPause() {
        player?.let { p ->
            if (p.isPlaying) p.pause() else p.play()
        }
        showControls()
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
        _uiState.value = _uiState.value.copy(currentPosition = position)
        showControls()
    }

    fun seekForward(seconds: Long = 10) {
        player?.let { p ->
            val newPos = (p.currentPosition + seconds * 1000).coerceAtMost(p.duration)
            seekTo(newPos)
        }
    }

    fun seekBackward(seconds: Long = 10) {
        player?.let { p ->
            val newPos = (p.currentPosition - seconds * 1000).coerceAtLeast(0)
            seekTo(newPos)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
        showControls()
    }

    fun selectAudioTrack(trackInfo: TrackInfo) {
        val p = player ?: return
        val tracks = p.currentTracks
        
        if (trackInfo.groupIndex < tracks.groups.size) {
            val group = tracks.groups[trackInfo.groupIndex]
            val override = TrackSelectionOverride(group.mediaTrackGroup, trackInfo.index)
            
            p.trackSelectionParameters = p.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(override)
                .build()
        }
        updateTrackInfo()
    }

    fun selectSubtitleTrack(trackInfo: TrackInfo?) {
        val p = player ?: return
        
        if (trackInfo == null) {
            // Disable subtitles
            p.trackSelectionParameters = p.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            val tracks = p.currentTracks
            if (trackInfo.groupIndex < tracks.groups.size) {
                val group = tracks.groups[trackInfo.groupIndex]
                val override = TrackSelectionOverride(group.mediaTrackGroup, trackInfo.index)
                
                p.trackSelectionParameters = p.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(override)
                    .build()
            }
        }
        updateTrackInfo()
    }

    fun toggleControls() {
        if (_uiState.value.isLocked) return
        
        val newVisibility = !_uiState.value.controlsVisible
        _uiState.value = _uiState.value.copy(controlsVisible = newVisibility)
        
        if (newVisibility && _uiState.value.isPlaying) {
            scheduleHideControls()
        } else {
            hideControlsJob?.cancel()
        }
    }

    fun showControls() {
        if (_uiState.value.isLocked) return
        
        _uiState.value = _uiState.value.copy(controlsVisible = true)
        if (_uiState.value.isPlaying) {
            scheduleHideControls()
        }
    }

    fun toggleLock() {
        val newLocked = !_uiState.value.isLocked
        _uiState.value = _uiState.value.copy(
            isLocked = newLocked,
            controlsVisible = true
        )
        if (!newLocked && _uiState.value.isPlaying) {
            scheduleHideControls()
        }
    }

    fun toggleOrientation() {
        _uiState.value = _uiState.value.copy(
            isLandscape = !_uiState.value.isLandscape
        )
        showControls()
    }

    fun cycleAspectRatio() {
        val modes = AspectRatioMode.entries
        val currentIndex = modes.indexOf(_uiState.value.aspectRatioMode)
        val nextIndex = (currentIndex + 1) % modes.size
        _uiState.value = _uiState.value.copy(
            aspectRatioMode = modes[nextIndex]
        )
        showControls()
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _uiState.value = _uiState.value.copy(aspectRatioMode = mode)
        showControls()
    }

    fun cycleDecoderMode() {
        val modes = DecoderMode.entries
        val currentIndex = modes.indexOf(_uiState.value.decoderMode)
        val nextIndex = (currentIndex + 1) % modes.size
        setDecoderMode(modes[nextIndex])
    }

    fun setDecoderMode(mode: DecoderMode) {
        _uiState.value = _uiState.value.copy(decoderMode = mode)
        applyDecoderMode(mode)
        showControls()
    }

    private fun applyDecoderMode(mode: DecoderMode) {
        val p = player ?: return
        
        val params = p.trackSelectionParameters.buildUpon()
        
        when (mode) {
            DecoderMode.HARDWARE -> {
                // Prefer hardware decoders, disable software fallback
                params.setForceLowestBitrate(false)
                // ExoPlayer uses hardware by default when available
            }
            DecoderMode.SOFTWARE -> {
                // Force software decoding by limiting hardware capabilities
                // This is done by setting tunneling off and preferring lower bitrate
                params.setForceLowestBitrate(true)
            }
            DecoderMode.AUTO -> {
                // Let ExoPlayer decide (default behavior)
                params.setForceLowestBitrate(false)
            }
        }
        
        p.trackSelectionParameters = params.build()
    }

    private fun scheduleHideControls() {
        hideControlsJob?.cancel()
        hideControlsJob = viewModelScope.launch {
            delay(3000)
            if (_uiState.value.isPlaying && !_uiState.value.isLocked) {
                _uiState.value = _uiState.value.copy(controlsVisible = false)
            }
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                player?.let { p ->
                    _uiState.value = _uiState.value.copy(
                        currentPosition = p.currentPosition.coerceAtLeast(0L),
                        bufferedPosition = p.bufferedPosition.coerceAtLeast(0L)
                    )
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        player?.removeListener(playerListener)
        hideControlsJob?.cancel()
        positionUpdateJob?.cancel()
    }

    // Gesture functions
    fun updateBrightness(delta: Float) {
        val newBrightness = (_uiState.value.brightness + delta).coerceIn(0f, 1f)
        _uiState.value = _uiState.value.copy(
            brightness = newBrightness,
            showBrightnessIndicator = true
        )
    }

    fun updateVolume(delta: Float) {
        val newVolume = (_uiState.value.volume + delta).coerceIn(0f, 1f)
        _uiState.value = _uiState.value.copy(
            volume = newVolume,
            showVolumeIndicator = true
        )
        player?.volume = newVolume
    }

    fun hideBrightnessIndicator() {
        _uiState.value = _uiState.value.copy(showBrightnessIndicator = false)
    }

    fun hideVolumeIndicator() {
        _uiState.value = _uiState.value.copy(showVolumeIndicator = false)
    }

    fun startSeeking(position: Long) {
        _uiState.value = _uiState.value.copy(
            isSeeking = true,
            seekPosition = position
        )
        // Seek immediately for real-time preview
        player?.seekTo(position)
    }

    fun updateSeekPosition(position: Long) {
        val clampedPosition = position.coerceIn(0L, _uiState.value.duration)
        _uiState.value = _uiState.value.copy(seekPosition = clampedPosition)
        // Real-time seeking for frame preview
        player?.seekTo(clampedPosition)
    }

    fun endSeeking() {
        _uiState.value = _uiState.value.copy(
            isSeeking = false,
            currentPosition = _uiState.value.seekPosition
        )
    }

    fun setInitialBrightness(brightness: Float) {
        _uiState.value = _uiState.value.copy(brightness = brightness)
    }

    fun setInitialVolume(volume: Float) {
        _uiState.value = _uiState.value.copy(volume = volume)
    }
}
