package com.chintan992.xplayer.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chintan992.xplayer.ui.theme.BrandAccent

@Composable
fun SeekOverlay(
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
fun GestureIndicator(
    value: Float,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    max: Float = 1f
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            
            // Vertical Progress Bar
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                // Determine color based on boost
                val barColor = if (value > 1.0f && max > 1.0f) BrandAccent else Color.White

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight((value / max).coerceIn(0f, 1f))
                        .align(Alignment.BottomCenter)
                        .background(barColor)
                )

                // 100% Separator (only if max > 1.0)
                if (max > 1.0f) {
                    val separationFraction = 1.0f / max
                    // Draw a small line at this position
                    // We can use a Spacer/Box tailored to be at that height
                    // Position is from bottom, so alignment needs care or relative offset
                    // Using a Box that fills max width, small height, placed at correct vertical bias or offset
                    // Easiest is to use a Box with absolute offset or alignment
                    // But Box alignment is Top/Center/Bottom. 
                    // Let's use a box with fillMaxHeight(fraction) to "push" a line up? No.
                    // Let's simple place a Box aligned BottomCenter with bottom padding?
                    // height of bar is 150.dp.
                    // 100% is at separationFraction * 150.dp from bottom.
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 150.dp * separationFraction) 
                            // Padding pushes the content up. If we align Bottom, padding bottom moves it up.
                            // But padding applies to the content of THIS box? No, applied to the modifier chain.
                            // If we use absolute offset it's easier but hard with dp.
                            // Let's try: Align Bottom, offset y = -height * fraction
                            // The offset y is positive downwards.
                            // So offset y = -150.dp * fraction
                    ) {
                        // The actual line
                         Box(modifier = Modifier
                             .fillMaxSize()
                             .background(Color.Black.copy(alpha = 0.5f)))
                    }
                }
            }
        }
    }
}

@Composable
fun LockedOverlay(onUnlock: () -> Unit) {
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
                contentDescription = androidx.compose.ui.res.stringResource(com.chintan992.xplayer.R.string.player_unlock),
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
