package com.chintan992.xplayer

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.chintan992.xplayer.ui.theme.BrandAccent
import com.chintan992.xplayer.ui.theme.CinemaTheme
import com.chintan992.xplayer.player.ui.*
import com.chintan992.xplayer.player.abstraction.AspectRatioMode
import com.chintan992.xplayer.player.abstraction.DecoderMode

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.outlined.Watch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.widget.Toast
import android.net.Uri
import com.chintan992.xplayer.cast.WearableHelper
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.ContextThemeWrapper
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.changedToUp
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException


import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope

import androidx.compose.foundation.layout.aspectRatio
import androidx.media3.ui.AspectRatioFrameLayout

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun VideoPlayerScreen(
    player: ExoPlayer,
    videoTitle: String,
    videoUri: String?,
    videoId: String?,
    subtitleUri: Uri?,
    onBackPressed: () -> Unit,
    onEnterPip: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: PlayerViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSubtitleSearchDialog by remember { mutableStateOf(false) }

    // Smart Cast State
    var showDeviceDialog by remember { mutableStateOf(false) }
    var availableNodes by remember { mutableStateOf<List<com.google.android.gms.wearable.Node>>(emptyList()) }

    // Logic to handle cast click
    val handleCastClick = {
        scope.launch {
            val nodes = withContext(Dispatchers.IO) {
                WearableHelper.getConnectedVideoPlayers(context)
            }
            if (nodes.isEmpty()) {
                // Fallback: check if ANY node is connected but maybe missing app
                val allNodes = withContext(Dispatchers.IO) {
                   com.google.android.gms.tasks.Tasks.await(com.google.android.gms.wearable.Wearable.getNodeClient(context).connectedNodes)
                }
                
                if (allNodes.isNotEmpty()) {
                     Toast.makeText(context, context.getString(R.string.player_cast_app_missing), Toast.LENGTH_LONG).show()
                } else {
                     Toast.makeText(context, context.getString(R.string.player_cast_no_device), Toast.LENGTH_SHORT).show()
                }
            } else if (nodes.size == 1) {
                // Auto-cast to single device
                if (videoUri != null) {
                    WearableHelper.castVideoToWatch(context, Uri.parse(videoUri), uiState.videoTitle, nodes.first().id)
                }
            } else {
                // Multiple devices, show picker
                availableNodes = nodes
                showDeviceDialog = true
            }
        }
    }
    
    // Device Selection Dialog
    if (showDeviceDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceDialog = false },
            title = { Text(stringResource(R.string.player_cast_select_device)) },
            text = {
                LazyColumn {
                    items(availableNodes) { node ->
                        TextButton(
                            onClick = {
                                showDeviceDialog = false
                                scope.launch {
                                    if (videoUri != null) {
                                        WearableHelper.castVideoToWatch(context, Uri.parse(videoUri), uiState.videoTitle, node.id)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = node.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeviceDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val activity = context as? Activity

    // Initialize ViewModel with player
    LaunchedEffect(player) {
        viewModel.setPlayer(videoTitle, videoId)
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

    CinemaTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            with(sharedTransitionScope) {
                if (uiState.playerType == com.chintan992.xplayer.player.abstraction.PlayerType.MPV) {
                // MPV View
                AndroidView(
                    factory = { context ->
                        com.chintan992.xplayer.player.ui.MPVAndroidView(context).apply {
                            // MPV view setup if needed
                        }
                    },
                    update = { view ->
                        // Cast UniversalPlayer back to MPV wrapper to set surface delegate
                        (viewModel.player as? com.chintan992.xplayer.player.abstraction.MPVPlayerWrapper)?.let { mpvWrapper ->
                            view.player = mpvWrapper
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { viewModel.toggleControls() }
                )
            } else {
                // ExoPlayer View
                val keepScreenOnSetting = viewModel.isKeepScreenOnEnabled()
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            this.player = player
                            useController = false
                            keepScreenOn = keepScreenOnSetting
                            setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_NEVER)
                            resizeMode = when (uiState.aspectRatioMode) {
                                AspectRatioMode.FIT -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                AspectRatioMode.FILL -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                AspectRatioMode.ZOOM -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                AspectRatioMode.STRETCH -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                                AspectRatioMode.RATIO_16_9 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                                AspectRatioMode.RATIO_4_3 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                            }
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { playerView ->
                        playerView.player = player
                        playerView.keepScreenOn = keepScreenOnSetting
                        playerView.resizeMode = when (uiState.aspectRatioMode) {
                            AspectRatioMode.FIT -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                            AspectRatioMode.FILL -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            AspectRatioMode.ZOOM -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            AspectRatioMode.STRETCH -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                            AspectRatioMode.RATIO_16_9 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                            AspectRatioMode.RATIO_4_3 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { viewModel.toggleControls() }
                )
            }
            }


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
        var doubleTapSide by remember { mutableStateOf<DoubleTapSide?>(null) }
        var doubleTapKey by remember { mutableIntStateOf(0) }
        var isLongPressing by remember { mutableStateOf(false) }

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
                                    doubleTapSide = DoubleTapSide.LEFT
                                    doubleTapKey++
                                } else if (offset.x > width * 0.65f) {
                                    viewModel.seekForward()
                                    doubleTapSide = DoubleTapSide.RIGHT
                                    doubleTapKey++
                                } else {
                                    viewModel.togglePlayPause()
                                }
                            }
                        )
                    }
                }
                .pointerInput(uiState.isLocked) {
                    if (!uiState.isLocked) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            startDragX = down.position.x
                            var dragStarted = false
                            var longPressTriggered = false
                            dragMode = DragMode.NONE

                            // Launch long press timer
                            val longPressJob = scope.launch {
                                delay(500) // Long press timeout
                                if (!dragStarted) {
                                    longPressTriggered = true
                                    isLongPressing = true
                                    viewModel.startSpeedOverride()
                                }
                            }

                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break

                                    if (change.changedToUp()) {
                                        break
                                    }

                                    if (change.positionChanged()) {
                                        val dragAmount = change.position - down.position
                                        if (!dragStarted && !longPressTriggered) {
                                            if (dragAmount.getDistance() > viewConfiguration.touchSlop) {
                                                dragStarted = true
                                                longPressJob.cancel()
                                                
                                                // Initialize drag mode
                                                if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)) {
                                                    dragMode = DragMode.SEEK
                                                    viewModel.startSeeking(uiState.currentPosition)
                                                } else {
                                                    dragMode = if (startDragX < size.width / 2) {
                                                        DragMode.BRIGHTNESS
                                                    } else {
                                                        DragMode.VOLUME
                                                    }
                                                }
                                            }
                                        }

                                        if (dragStarted) {
                                            // Handle drag updates
                                            change.consume()
                                            // Note: we calculate delta from previous position, but here we used total dragAmount for detection
                                            // Ideally we want delta. Let's rely on change.position - change.previousPosition
                                            val delta = change.position - change.previousPosition
                                            
                                            when (dragMode) {
                                                DragMode.SEEK -> {
                                                    val seekDelta = (delta.x * 200).toLong()
                                                    viewModel.updateSeekPosition(uiState.seekPosition + seekDelta)
                                                }
                                                DragMode.BRIGHTNESS -> {
                                                    val brightnessDelta = -delta.y / 500f
                                                    viewModel.updateBrightness(brightnessDelta)
                                                }
                                                DragMode.VOLUME -> {
                                                    val volumeDelta = -delta.y / 500f
                                                    viewModel.updateVolume(volumeDelta)
                                                }
                                                else -> {}
                                            }
                                        }
                                    }
                                }
                            } finally {
                                longPressJob.cancel()
                                if (longPressTriggered) {
                                    viewModel.stopSpeedOverride()
                                    isLongPressing = false
                                }
                                if (dragStarted) {
                                    viewModel.endSeeking()
                                    dragMode = DragMode.NONE
                                }
                            }
                        }
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
        
        // Double Tap Animation Overlay
        if (doubleTapSide != null) {
            key(doubleTapKey) {
                DoubleTapOverlay(
                    side = doubleTapSide!!,
                    onDismiss = { doubleTapSide = null }
                )
            }
        }

        // Brightness indicator (Left Edge)
        AnimatedVisibility(
            visible = uiState.showBrightnessIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 48.dp)
        ) {
            GestureIndicator(
                value = uiState.brightness,
                icon = Icons.Rounded.WbSunny
            )
        }

        // Volume indicator (Right Edge)
        AnimatedVisibility(
            visible = uiState.showVolumeIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 48.dp)
        ) {
            GestureIndicator(
                value = uiState.volume,
                icon = Icons.AutoMirrored.Rounded.VolumeUp
            )
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
                val onPlayPause = remember(viewModel) { { viewModel.togglePlayPause() } }
                val onSeek = remember(viewModel) { { pos: Long -> viewModel.seekTo(pos) } }
                val onSeekForward = remember(viewModel) { { viewModel.seekForward() } }
                val onSeekBackward = remember(viewModel) { { viewModel.seekBackward() } }
                val onNext = remember(viewModel) { { viewModel.seekToNext() } }
                val onPrevious = remember(viewModel) { { viewModel.seekToPrevious() } }
                val onSpeedClick = remember { { showSpeedDialog = true } }
                val onDecoderClick = remember(viewModel) { { viewModel.cycleDecoderMode() } }
                val onAudioClick = remember { { showAudioDialog = true } }
                val onSubtitleClick = remember { { showSubtitleDialog = true } }
                val onLockClick = remember(viewModel) { { viewModel.toggleLock() } }
                val onAspectRatioClick = remember(viewModel) { { viewModel.cycleAspectRatio() } }
                val onOrientationClick = remember(viewModel) { { viewModel.toggleOrientation() } }

                ControlsOverlay(
                    uiState = uiState,
                    onBackPressed = onBackPressed,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    onSeekForward = onSeekForward,
                    onSeekBackward = onSeekBackward,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onSpeedClick = onSpeedClick,
                    onDecoderClick = onDecoderClick,
                    onAudioClick = onAudioClick,
                    onSubtitleClick = onSubtitleClick,
                    onLockClick = onLockClick,
                    onAspectRatioClick = onAspectRatioClick,
                    onOrientationClick = {
                        viewModel.toggleOrientation()
                    },
                    onPipClick = onEnterPip,
                    onWatchCastClick = {
                        handleCastClick()
                    },
                    onSwitchPlayerClick = {
                        val newType = if (uiState.playerType == com.chintan992.xplayer.player.abstraction.PlayerType.EXO) 
                            com.chintan992.xplayer.player.abstraction.PlayerType.MPV 
                        else 
                            com.chintan992.xplayer.player.abstraction.PlayerType.EXO
                        viewModel.switchPlayer(newType)
                    }
                )
            }
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
            onSearchClick = {
                showSubtitleDialog = false
                showSubtitleSearchDialog = true
            },
            onDismiss = { showSubtitleDialog = false }
        )
    }

    if (showSubtitleSearchDialog) {
        SubtitleSearchDialog(
            results = uiState.subtitleSearchResults,
            isSearching = uiState.isSearchingSubtitles,
            onSearch = { viewModel.searchSubtitles(it) },
            onDownload = { 
                viewModel.downloadAndApplySubtitle(it)
                showSubtitleSearchDialog = false
            },
            onDismiss = { showSubtitleSearchDialog = false }
        )
    }

    // Error Dialog
    uiState.resolvingError?.let { error ->
        AlertDialog(
            onDismissRequest = { 
                viewModel.clearError()
                onBackPressed() // Navigate back on critical error
            },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearError()
                        onBackPressed()
                    }
                ) {
                    Text("OK")
                }
            }
        )
        
}
}

private enum class DragMode {
    NONE, BRIGHTNESS, VOLUME, SEEK
}
