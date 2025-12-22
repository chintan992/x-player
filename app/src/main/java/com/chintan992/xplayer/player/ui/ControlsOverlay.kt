package com.chintan992.xplayer.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chintan992.xplayer.PlayerUiState

@Composable
fun ControlsOverlay(
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
    onPipClick: () -> Unit,
    onWatchCastClick: () -> Unit
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
            onWatchCastClick = onWatchCastClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )

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
