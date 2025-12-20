package com.chintan992.xplayer

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.chintan992.xplayer.ui.theme.BrandAccent

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.*
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
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

@Composable
fun VideoPlayerScreen(
    player: ExoPlayer,
    videoTitle: String = "Video",
    videoUri: String? = null,
    videoId: String? = null,
    subtitleUri: android.net.Uri? = null,
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
    val activity = context as? Activity

    // Initialize ViewModel with player
    LaunchedEffect(player) {
        viewModel.setPlayer(player, videoTitle, videoId)
    }

    // Start Playback
    LaunchedEffect(videoUri, videoId) {
         if (videoUri != null) {
            viewModel.playMedia(videoUri, videoTitle, videoId, subtitleUri)
         }
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
                    keepScreenOn = true
                    setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_NEVER)
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

        // Loading Indicator
        if (uiState.isBuffering || uiState.isResolving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .pointerInput(Unit) {}, // Block clicks while loading
                contentAlignment = Alignment.Center
            ) {
                CustomLoadingIndicator()
            }
        }

        // Gesture detection overlay
        // We use a single surface to handle all gestures, enabling center double-tap and unified drag logic
        var dragMode by remember { mutableStateOf(DragMode.NONE) }
        var startDragX by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uiState.isLocked) {
                    if (!uiState.isLocked) {
                        detectTapGestures(
                            onTap = { viewModel.toggleControls() },
                            onDoubleTap = { offset ->
                                val width = size.width
                                if (offset.x < width * 0.35f) {
                                    viewModel.seekBackward()
                                } else if (offset.x > width * 0.65f) {
                                    viewModel.seekForward()
                                } else {
                                    viewModel.togglePlayPause()
                                }
                            },
                            onPress = {
                                try {
                                    withTimeout(500) {
                                        tryAwaitRelease()
                                    }
                                } catch (e: TimeoutCancellationException) {
                                    viewModel.startSpeedOverride()
                                    tryAwaitRelease()
                                    viewModel.stopSpeedOverride()
                                }
                            }
                        )
                    }
                }
                .pointerInput(uiState.isLocked) {
                    if (!uiState.isLocked) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                startDragX = offset.x
                                dragMode = DragMode.NONE
                            },
                            onDragEnd = {
                                viewModel.endSeeking()
                                dragMode = DragMode.NONE
                            },
                            onDragCancel = {
                                viewModel.endSeeking()
                                dragMode = DragMode.NONE
                            },
                            onDrag = { change, dragAmount ->
                                // Determine mode if not set
                                if (dragMode == DragMode.NONE) {
                                    if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)) {
                                        dragMode = DragMode.SEEK
                                        viewModel.startSeeking(uiState.currentPosition)
                                    } else {
                                        // Vertical
                                        dragMode = if (startDragX < size.width / 2) {
                                            DragMode.BRIGHTNESS
                                        } else {
                                            DragMode.VOLUME
                                        }
                                    }
                                }

                                when (dragMode) {
                                    DragMode.SEEK -> {
                                        // 1px = 200ms
                                        val seekDelta = (dragAmount.x * 200).toLong()
                                        viewModel.updateSeekPosition(uiState.seekPosition + seekDelta)
                                    }
                                    DragMode.BRIGHTNESS -> {
                                        val delta = -dragAmount.y / 500f
                                        viewModel.updateBrightness(delta)
                                    }
                                    DragMode.VOLUME -> {
                                        val delta = -dragAmount.y / 500f
                                        viewModel.updateVolume(delta)
                                    }
                                    else -> {} // Should not happen
                                }
                            }
                        )
                    }
                }
        )

        // Center Seek Overlay
        AnimatedVisibility(
            visible = uiState.isSeeking,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            SeekOverlay(
                current = uiState.seekPosition,
                duration = uiState.duration
            )
        }

        // Brightness indicator (Left Edge)
        AnimatedVisibility(
            visible = uiState.showBrightnessIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 32.dp)
        ) {
            SlimIndicator(value = uiState.brightness)
        }

        // Volume indicator (Right Edge)
        AnimatedVisibility(
            visible = uiState.showVolumeIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 32.dp)
        ) {
            SlimIndicator(value = uiState.volume)
        }

        // Speed Override Indicator
        AnimatedVisibility(
            visible = uiState.isSpeedOverridden,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "2x Speed",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // ... controls overlay uses same state ...
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
                    onNext = { viewModel.seekToNext() },
                    onPrevious = { viewModel.seekToPrevious() },
                    onSpeedClick = { showSpeedDialog = true },
                    onDecoderClick = { viewModel.cycleDecoderMode() },
                    onAudioClick = { showAudioDialog = true },
                    onSubtitleClick = { showSubtitleDialog = true },
                    onLockClick = { viewModel.toggleLock() },
                    onAspectRatioClick = { viewModel.cycleAspectRatio() },
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
}

private enum class DragMode {
    NONE, BRIGHTNESS, VOLUME, SEEK
}

@Composable
private fun SeekOverlay(
    current: Long,
    duration: Long
) {
    Column(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${formatTime(current)} / ${formatTime(duration)}",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}



@Composable
private fun SlimIndicator(
    value: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(6.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(value.coerceIn(0f, 1f))
                .align(Alignment.BottomCenter)
                .background(Color.White)
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
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSpeedClick: () -> Unit,
    onDecoderClick: () -> Unit,
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

    var showInlineSettings by remember { mutableStateOf(false) }
    
    // Hide inline settings if controls fade out
    LaunchedEffect(uiState.controlsVisible) {
        if (!uiState.controlsVisible) {
            showInlineSettings = false
        }
    }

    // ... (rest of TopBar logic)

        // Top bar
        TopBar(
            title = uiState.videoTitle,
            onBackPressed = onBackPressed,
            showInlineSettings = showInlineSettings,
            onSettingsToggle = { showInlineSettings = !showInlineSettings },
            playbackSpeed = uiState.playbackSpeed,
            decoderMode = uiState.decoderMode,
            hasAudioTracks = uiState.audioTracks.size > 1,
            hasSubtitles = uiState.subtitleTracks.isNotEmpty(),
            onSpeedClick = onSpeedClick,
            onDecoderClick = onDecoderClick,
            onAudioClick = onAudioClick,
            onSubtitleClick = onSubtitleClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )

    // ... (Remove settings sheet call)
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
                onPreviousClick = onPrevious,
                onNextClick = onNext,
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
    showInlineSettings: Boolean,
    onSettingsToggle: () -> Unit,
    playbackSpeed: Float,
    decoderMode: DecoderMode,
    hasAudioTracks: Boolean,
    hasSubtitles: Boolean,
    onSpeedClick: () -> Unit,
    onDecoderClick: () -> Unit,
    onAudioClick: () -> Unit,
    onSubtitleClick: () -> Unit,
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

        if (showInlineSettings) {
             Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                 // Inline Controls (Right to Left)
                 
                 if (hasSubtitles) {
                    IconButton(onClick = onSubtitleClick) {
                        Icon(
                            imageVector = Icons.Outlined.Subtitles,
                            contentDescription = "Subtitles",
                            tint = Color.White
                        )
                    }
                }
                
                if (hasAudioTracks) {
                    IconButton(onClick = onAudioClick) {
                         Icon(
                            imageVector = Icons.Outlined.Headphones,
                            contentDescription = "Audio",
                            tint = Color.White
                        )
                    }
                }

                // Decoder (Text Badge)
                TextButton(onClick = onDecoderClick) {
                    Text(
                        text = when (decoderMode) {
                            DecoderMode.HARDWARE -> "HW"
                            DecoderMode.SOFTWARE -> "SW"
                            DecoderMode.AUTO -> "AU"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(onClick = onSpeedClick) {
                     Icon(
                        imageVector = Icons.Outlined.SlowMotionVideo,
                        contentDescription = "Speed",
                        tint = Color.White
                    )
                }
            }
        } else {
             Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        
        IconButton(onClick = onSettingsToggle) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = if (showInlineSettings) BrandAccent else Color.White
            )
        }
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
            modifier = Modifier.fillMaxWidth(),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color.White, CircleShape)
                )
            },
            track = { sliderState ->
                val fraction = sliderState.value
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(BrandAccent)
                    )
                }
            }
        )
    }
}

@Composable
private fun BottomControls(
    aspectRatioMode: AspectRatioMode,
    isLandscape: Boolean,
    onLockClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onOrientationClick: () -> Unit,
    onPipClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side - Lock and Navigation buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLockClick) {
                Icon(
                    imageVector = Icons.Outlined.LockOpen,
                    contentDescription = "Lock Screen",
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(onClick = onPreviousClick) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Video",
                    tint = Color.White
                )
            }
            
            IconButton(onClick = onNextClick) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Video",
                    tint = Color.White
                )
            }
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
                    imageVector = Icons.Outlined.AspectRatio,
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
                    imageVector = Icons.Outlined.ScreenRotation,
                    contentDescription = if (isLandscape) "Switch to Portrait" else "Switch to Landscape",
                    tint = Color.White
                )
            }

            // PiP button
            IconButton(onClick = onPipClick) {
                Icon(
                    imageVector = Icons.Outlined.PictureInPicture,
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
                            color = if (speed == currentSpeed) BrandAccent else MaterialTheme.colorScheme.onSurface
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
private fun <T> CustomSelectionDialog(
    title: String,
    items: List<T>,
    selectedItem: T?,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E),
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.heightIn(min = 50.dp, max = 400.dp)
                ) {
                    items(items) { item ->
                        val isSelected = item == selectedItem
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemSelected(item) }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BrandAccent,
                                    unselectedColor = Color.White.copy(alpha = 0.6f)
                                )
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = itemLabel(item),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = BrandAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackSelectorDialog(
    title: String,
    tracks: List<TrackInfo>,
    onTrackSelected: (TrackInfo) -> Unit,
    onDismiss: () -> Unit
) {
    // Find currently selected track
    val selectedTrack = tracks.find { it.isSelected }

    CustomSelectionDialog(
        title = title,
        items = tracks,
        selectedItem = selectedTrack,
        itemLabel = { "${it.name}${if (it.language != null) " (${it.language})" else ""}" },
        onItemSelected = onTrackSelected,
        onDismiss = onDismiss
    )
}

@Composable
private fun SubtitleSelectorDialog(
    tracks: List<TrackInfo>,
    onTrackSelected: (TrackInfo?) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf<TrackInfo?>(null) + tracks
    val selectedTrack = tracks.find { it.isSelected } ?: options.first()

    CustomSelectionDialog(
        title = "Subtitles",
        items = options,
        selectedItem = selectedTrack,
        itemLabel = { track -> 
            track?.let { "${it.name}${if (it.language != null) " (${it.language})" else ""}" } ?: "Off"
        },
        onItemSelected = onTrackSelected,
        onDismiss = onDismiss
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
                            color = if (mode == currentMode) BrandAccent else MaterialTheme.colorScheme.onSurface
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
@Composable
private fun CustomLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = BrandAccent
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "angle"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier.size(64.dp)) {
        // Rotating Arc
        drawArc(
            color = color,
            startAngle = angle,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Pulsing Dot
        drawCircle(
            color = color.copy(alpha = 0.5f),
            radius = 8.dp.toPx() * pulse,
            center = center
        )
        drawCircle(
            color = color,
            radius = 6.dp.toPx(),
            center = center
        )
    }
}
