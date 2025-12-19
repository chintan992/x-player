package com.chintan992.xplayer

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    player: ExoPlayer,
    videoTitle: String = "Video",
    onBackPressed: () -> Unit = {},
    onEnterPip: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: PlayerViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showAspectRatioDialog by remember { mutableStateOf(false) }
    val activity = context as? Activity

    // Initialize ViewModel with player
    LaunchedEffect(player) {
        viewModel.setPlayer(player, videoTitle)
    }

    // Handle back press
    BackHandler {
        onBackPressed()
    }

    // Orientation and immersive mode
    LaunchedEffect(uiState.isLandscape) {
        activity?.requestedOrientation = if (uiState.isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
    }

    DisposableEffect(Unit) {
        val window = activity?.window
        val decorView = window?.decorView

        // Enter immersive mode
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window?.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            decorView?.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }

        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window?.insetsController?.show(WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    // Initialize brightness and volume from system
    LaunchedEffect(Unit) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        viewModel.setInitialVolume(currentVolume / maxVolume)
        
        activity?.window?.attributes?.let { attrs ->
            val brightness = if (attrs.screenBrightness < 0) 0.5f else attrs.screenBrightness
            viewModel.setInitialBrightness(brightness)
        }
    }

    // Apply brightness to window
    LaunchedEffect(uiState.brightness) {
        activity?.window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.screenBrightness = uiState.brightness
            window.attributes = layoutParams
        }
    }

    // Apply volume to system
    LaunchedEffect(uiState.volume) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (uiState.volume * maxVolume).toInt(),
            0
        )
    }

    // Hide indicators after delay
    LaunchedEffect(uiState.showBrightnessIndicator) {
        if (uiState.showBrightnessIndicator) {
            delay(1000)
            viewModel.hideBrightnessIndicator()
        }
    }

    LaunchedEffect(uiState.showVolumeIndicator) {
        if (uiState.showVolumeIndicator) {
            delay(1000)
            viewModel.hideVolumeIndicator()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video surface
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { playerView ->
                playerView.resizeMode = when (uiState.aspectRatioMode) {
                    AspectRatioMode.FIT -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FILL -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.ZOOM -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.STRETCH -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.RATIO_16_9 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    AspectRatioMode.RATIO_4_3 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                }
            }
        )

        // Gesture detection overlay
        Row(modifier = Modifier.fillMaxSize()) {
            // Left side - Brightness gesture
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(uiState.isLocked) {
                        if (!uiState.isLocked) {
                            detectDragGestures(
                                onDrag = { _, dragAmount ->
                                    val delta = -dragAmount.y / 500f
                                    viewModel.updateBrightness(delta)
                                }
                            )
                        }
                    }
                    .pointerInput(uiState.isLocked) {
                        if (!uiState.isLocked) {
                            detectTapGestures(
                                onTap = { viewModel.toggleControls() },
                                onDoubleTap = { viewModel.seekBackward() }
                            )
                        }
                    }
            )

            // Right side - Volume gesture
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(uiState.isLocked) {
                        if (!uiState.isLocked) {
                            detectDragGestures(
                                onDrag = { _, dragAmount ->
                                    val delta = -dragAmount.y / 500f
                                    viewModel.updateVolume(delta)
                                }
                            )
                        }
                    }
                    .pointerInput(uiState.isLocked) {
                        if (!uiState.isLocked) {
                            detectTapGestures(
                                onTap = { viewModel.toggleControls() },
                                onDoubleTap = { viewModel.seekForward() }
                            )
                        }
                    }
            )
        }

        // Brightness indicator
        AnimatedVisibility(
            visible = uiState.showBrightnessIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 48.dp)
        ) {
            GestureIndicator(
                icon = when {
                    uiState.brightness < 0.33f -> Icons.Default.BrightnessLow
                    uiState.brightness < 0.66f -> Icons.Default.BrightnessMedium
                    else -> Icons.Default.BrightnessHigh
                },
                value = uiState.brightness,
                label = "${(uiState.brightness * 100).toInt()}%"
            )
        }

        // Volume indicator
        AnimatedVisibility(
            visible = uiState.showVolumeIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 48.dp)
        ) {
            GestureIndicator(
                icon = when {
                    uiState.volume < 0.01f -> Icons.Default.VolumeMute
                    uiState.volume < 0.5f -> Icons.Default.VolumeDown
                    else -> Icons.Default.VolumeUp
                },
                value = uiState.volume,
                label = "${(uiState.volume * 100).toInt()}%"
            )
        }

        // Controls overlay
        AnimatedVisibility(
            visible = uiState.controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (uiState.isLocked) {
                LockedOverlay(onUnlock = { viewModel.toggleLock() })
            } else {
                ControlsOverlay(
                    uiState = uiState,
                    onBackPressed = onBackPressed,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onSeek = { viewModel.seekTo(it) },
                    onSeekForward = { viewModel.seekForward() },
                    onSeekBackward = { viewModel.seekBackward() },
                    onSpeedClick = { showSpeedDialog = true },
                    onAudioClick = { showAudioDialog = true },
                    onSubtitleClick = { showSubtitleDialog = true },
                    onLockClick = { viewModel.toggleLock() },
                    onAspectRatioClick = { showAspectRatioDialog = true },
                    onOrientationClick = { viewModel.toggleOrientation() },
                    onPipClick = onEnterPip
                )
            }
        }
    }

    // Dialogs
    if (showSpeedDialog) {
        SpeedSelectorDialog(
            currentSpeed = uiState.playbackSpeed,
            onSpeedSelected = {
                viewModel.setPlaybackSpeed(it)
                showSpeedDialog = false
            },
            onDismiss = { showSpeedDialog = false }
        )
    }

    if (showAudioDialog) {
        TrackSelectorDialog(
            title = "Audio Track",
            tracks = uiState.audioTracks,
            onTrackSelected = {
                viewModel.selectAudioTrack(it)
                showAudioDialog = false
            },
            onDismiss = { showAudioDialog = false }
        )
    }

    if (showSubtitleDialog) {
        SubtitleSelectorDialog(
            tracks = uiState.subtitleTracks,
            onTrackSelected = {
                viewModel.selectSubtitleTrack(it)
                showSubtitleDialog = false
            },
            onDismiss = { showSubtitleDialog = false }
        )
    }

    if (showAspectRatioDialog) {
        AspectRatioSelectorDialog(
            currentMode = uiState.aspectRatioMode,
            onModeSelected = {
                viewModel.setAspectRatio(it)
                showAspectRatioDialog = false
            },
            onDismiss = { showAspectRatioDialog = false }
        )
    }
}

@Composable
private fun GestureIndicator(
    icon: ImageVector,
    value: Float,
    label: String
) {
    Column(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { value },
            modifier = Modifier
                .width(60.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun LockedOverlay(onUnlock: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        IconButton(
            onClick = onUnlock,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Unlock",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun ControlsOverlay(
    uiState: PlayerUiState,
    onBackPressed: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSpeedClick: () -> Unit,
    onAudioClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onLockClick: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onOrientationClick: () -> Unit,
    onPipClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
        )

        // Bottom gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )

        // Top bar
        TopBar(
            title = uiState.videoTitle,
            onBackPressed = onBackPressed,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Control buttons row
        ControlButtonsRow(
            playbackSpeed = uiState.playbackSpeed,
            hasAudioTracks = uiState.audioTracks.size > 1,
            hasSubtitles = uiState.subtitleTracks.isNotEmpty(),
            onSpeedClick = onSpeedClick,
            onAudioClick = onAudioClick,
            onSubtitleClick = onSubtitleClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 60.dp, start = 16.dp)
        )

        // Center play/pause controls
        CenterControls(
            isPlaying = uiState.isPlaying,
            onPlayPause = onPlayPause,
            onSeekForward = onSeekForward,
            onSeekBackward = onSeekBackward,
            modifier = Modifier.align(Alignment.Center)
        )

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Seek bar
            SeekBar(
                currentPosition = uiState.currentPosition,
                duration = uiState.duration,
                bufferedPosition = uiState.bufferedPosition,
                onSeek = onSeek
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom buttons
            BottomControls(
                aspectRatioMode = uiState.aspectRatioMode,
                isLandscape = uiState.isLandscape,
                onLockClick = onLockClick,
                onAspectRatioClick = onAspectRatioClick,
                onOrientationClick = onOrientationClick,
                onPipClick = onPipClick
            )
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackPressed) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ControlButtonsRow(
    playbackSpeed: Float,
    hasAudioTracks: Boolean,
    hasSubtitles: Boolean,
    onSpeedClick: () -> Unit,
    onAudioClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ControlButton(
            icon = Icons.Default.SlowMotionVideo,
            label = "${playbackSpeed}x",
            onClick = onSpeedClick
        )

        if (hasAudioTracks) {
            ControlButton(
                icon = Icons.Default.Headphones,
                label = "Audio",
                onClick = onAudioClick
            )
        }

        if (hasSubtitles) {
            ControlButton(
                icon = Icons.Default.Subtitles,
                label = "Subs",
                onClick = onSubtitleClick
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun CenterControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onSeekBackward) {
            Icon(
                imageVector = Icons.Default.FastRewind,
                contentDescription = "Rewind 10s",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(64.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        IconButton(onClick = onSeekForward) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "Forward 10s",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeekBar(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    onSeek: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "-${formatTime(duration - currentPosition)}",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Slider(
            value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
            onValueChange = { fraction ->
                onSeek((fraction * duration).toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BottomControls(
    aspectRatioMode: AspectRatioMode,
    isLandscape: Boolean,
    onLockClick: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onOrientationClick: () -> Unit,
    onPipClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side - Lock button
        IconButton(onClick = onLockClick) {
            Icon(
                imageVector = Icons.Default.LockOpen,
                contentDescription = "Lock Screen",
                tint = Color.White
            )
        }

        // Right side - Aspect ratio, orientation, PiP
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Aspect ratio button with current mode label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onAspectRatioClick)
            ) {
                Icon(
                    imageVector = Icons.Default.AspectRatio,
                    contentDescription = "Aspect Ratio",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = aspectRatioMode.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            // Orientation toggle button
            IconButton(onClick = onOrientationClick) {
                Icon(
                    imageVector = Icons.Default.ScreenRotation,
                    contentDescription = if (isLandscape) "Switch to Portrait" else "Switch to Landscape",
                    tint = Color.White
                )
            }

            // PiP button
            IconButton(onClick = onPipClick) {
                Icon(
                    imageVector = Icons.Default.PictureInPicture,
                    contentDescription = "Picture in Picture",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun SpeedSelectorDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Speed") },
        text = {
            LazyColumn {
                items(speeds) { speed ->
                    TextButton(
                        onClick = { onSpeedSelected(speed) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${speed}x",
                            fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal,
                            color = if (speed == currentSpeed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TrackSelectorDialog(
    title: String,
    tracks: List<TrackInfo>,
    onTrackSelected: (TrackInfo) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (tracks.isEmpty()) {
                Text("No tracks available")
            } else {
                LazyColumn {
                    items(tracks) { track ->
                        TextButton(
                            onClick = { onTrackSelected(track) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${track.name}${if (track.language != null) " (${track.language})" else ""}",
                                fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (track.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SubtitleSelectorDialog(
    tracks: List<TrackInfo>,
    onTrackSelected: (TrackInfo?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Subtitles") },
        text = {
            LazyColumn {
                item {
                    TextButton(
                        onClick = { onTrackSelected(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Off")
                    }
                }
                items(tracks) { track ->
                    TextButton(
                        onClick = { onTrackSelected(track) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${track.name}${if (track.language != null) " (${track.language})" else ""}",
                            fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (track.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AspectRatioSelectorDialog(
    currentMode: AspectRatioMode,
    onModeSelected: (AspectRatioMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aspect Ratio") },
        text = {
            LazyColumn {
                items(AspectRatioMode.entries.toList()) { mode ->
                    TextButton(
                        onClick = { onModeSelected(mode) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = mode.displayName,
                            fontWeight = if (mode == currentMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (mode == currentMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
