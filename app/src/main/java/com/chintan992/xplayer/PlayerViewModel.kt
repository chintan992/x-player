package com.chintan992.xplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
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
    val seekPosition: Long = 0L,
    val isSpeedOverridden: Boolean = false,
    val isResolving: Boolean = false,
    val resolvingError: String? = null,
    val isBuffering: Boolean = false
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackPositionManager: PlaybackPositionManager,
    private val headerStorage: HeaderStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var player: ExoPlayer? = null
    private var hideControlsJob: Job? = null
    private var positionUpdateJob: Job? = null
    private var currentVideoId: String? = null
    private var originalSpeed: Float = 1f

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            if (isPlaying) {
                startPositionUpdates()
                scheduleHideControls()
            } else {
                stopPositionUpdates()
                // Save position when paused
                saveCurrentPosition()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.value = _uiState.value.copy(
                isBuffering = playbackState == Player.STATE_BUFFERING
            )

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

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.localConfiguration?.tag?.let { tag ->
                if (tag is String) {
                    _uiState.value = _uiState.value.copy(videoTitle = tag)
                }
            }
            // Update duration when media item changes
            player?.let { p ->
                _uiState.value = _uiState.value.copy(
                    duration = p.duration.coerceAtLeast(0L),
                    currentPosition = 0L
                )
            }
            updateTrackInfo()
        }
    }

    fun setPlayer(exoPlayer: ExoPlayer, videoTitle: String, videoId: String? = null) {
        player = exoPlayer
        currentVideoId = videoId
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
        
        // Restore saved position if available
        videoId?.let { id ->
            viewModelScope.launch {
                val savedPosition = playbackPositionManager.getPosition(id)
                if (savedPosition > 0 && savedPosition < exoPlayer.duration) {
                    exoPlayer.seekTo(savedPosition)
                    _uiState.value = _uiState.value.copy(currentPosition = savedPosition)
                }
            }
        }
    }
    
    private fun saveCurrentPosition() {
        val p = player ?: return
        val id = currentVideoId ?: return
        val position = p.currentPosition
        val duration = p.duration
        
        viewModelScope.launch {
            playbackPositionManager.savePosition(id, position, duration)
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

    fun seekToNext() {
        if (player?.hasNextMediaItem() == true) {
            player?.seekToNextMediaItem()
            showControls()
        }
    }

    fun seekToPrevious() {
        if (player?.hasPreviousMediaItem() == true) {
            player?.seekToPreviousMediaItem()
            showControls()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        if (!_uiState.value.isSpeedOverridden) {
            player?.setPlaybackSpeed(speed)
            _uiState.value = _uiState.value.copy(playbackSpeed = speed)
            showControls()
        }
    }

    fun startSpeedOverride() {
        if (!_uiState.value.isSpeedOverridden) {
            originalSpeed = _uiState.value.playbackSpeed
            player?.setPlaybackSpeed(2f)
            _uiState.value = _uiState.value.copy(
                isSpeedOverridden = true,
                playbackSpeed = 2f
            )
        }
    }

    fun stopSpeedOverride() {
        if (_uiState.value.isSpeedOverridden) {
            player?.setPlaybackSpeed(originalSpeed)
            _uiState.value = _uiState.value.copy(
                isSpeedOverridden = false,
                playbackSpeed = originalSpeed
            )
        }
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

    // Resolution Logic
    // Placeholder for resolvers - will be injected or populated later
    private val resolvers: List<com.chintan992.xplayer.resolver.StreamResolver> = emptyList()

    fun playMedia(url: String, videoTitle: String, videoId: String? = null, subtitleUri: android.net.Uri? = null) {
        // Reset state
        _uiState.value = _uiState.value.copy(
            isResolving = false,
            resolvingError = null,
            videoTitle = videoTitle
        )
        currentVideoId = videoId

        // Check if this video is part of the current playlist in PlaylistManager
        val playlist = PlaylistManager.currentPlaylist
        val playlistIndex = if (videoId != null && playlist.isNotEmpty()) {
            playlist.indexOfFirst { it.id.toString() == videoId }
        } else -1

        if (playlistIndex != -1) {
            // Play from playlist
            playPlaylist(playlist, playlistIndex)
        } else {
            // Single item flow (Resolution or Direct)
            val resolver = resolvers.find { it.canResolve(url) }
            
            if (resolver != null) {
                resolveAndPlay(resolver, url, subtitleUri)
            } else {
                // Treat as direct link
                playDirectly(url, videoTitle, emptyMap(), subtitleUri)
            }
        }
    }

    private fun playPlaylist(playlist: List<VideoItem>, startIndex: Int) {
        player?.let { p ->
            val mediaItems = playlist.map { video ->
                createMediaItem(video.uri.toString(), video.name, video.subtitleUri)
            }
            p.setMediaItems(mediaItems, startIndex, 0L)
            p.prepare()
            p.play()
        }
    }

    private fun resolveAndPlay(resolver: com.chintan992.xplayer.resolver.StreamResolver, url: String, subtitleUri: android.net.Uri?) {
        viewModelScope.launch {
            resolver.resolve(url).collect { resource ->
                when (resource) {
                    is com.chintan992.xplayer.resolver.Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isResolving = true,
                            resolvingError = null
                        )
                    }
                    is com.chintan992.xplayer.resolver.Resource.Success -> {
                        _uiState.value = _uiState.value.copy(isResolving = false)
                        val config = resource.data
                        
                        // Update headers if present
                        if (config.headers.isNotEmpty()) {
                            // Extract host from URL
                            try {
                                val host = java.net.URI(config.url).host
                                if (host != null) {
                                    headerStorage.addHeaders(host, config.headers)
                                }
                            } catch (e: Exception) {
                                // Ignore invalid URI for header storage purposes
                            }
                        }

                        // Play the resolved URL
                        playDirectly(config.url, _uiState.value.videoTitle, config.headers, subtitleUri)
                    }
                    is com.chintan992.xplayer.resolver.Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isResolving = false,
                            resolvingError = resource.message
                        )
                    }
                }
            }
        }
    }

    private fun playDirectly(url: String, title: String, headers: Map<String, String>, subtitleUri: android.net.Uri? = null) {
        player?.let { p ->
            // Note: Headers are handled by the Interceptor/HeaderStorage
            if (headers.isNotEmpty()) {
                 try {
                    val host = java.net.URI(url).host
                    if (host != null) {
                        headerStorage.addHeaders(host, headers)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }

            val mediaItem = createMediaItem(url, title, subtitleUri)
            p.setMediaItem(mediaItem)
            p.prepare()
            p.play()
        }
    }

    private fun createMediaItem(url: String, title: String, subtitleUri: android.net.Uri?): MediaItem {
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(url)
            .setTag(title)
        
        if (subtitleUri != null) {
            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                .apply {
                    val path = subtitleUri.path
                    if (path != null) {
                        if (path.endsWith(".ass", ignoreCase = true) || path.endsWith(".ssa", ignoreCase = true)) {
                            setMimeType(androidx.media3.common.MimeTypes.TEXT_SSA)
                        } else if (path.endsWith(".vtt", ignoreCase = true)) {
                            setMimeType(androidx.media3.common.MimeTypes.TEXT_VTT)
                        } else {
                            setMimeType(androidx.media3.common.MimeTypes.APPLICATION_SUBRIP)
                        }
                    } else {
                         setMimeType(androidx.media3.common.MimeTypes.APPLICATION_SUBRIP)
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
