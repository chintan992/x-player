package com.chintan992.xplayer.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chintan992.xplayer.AspectRatioMode

@Composable
fun BottomControls(
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
