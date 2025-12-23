package com.chintan992.xplayer.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.SlowMotionVideo
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chintan992.xplayer.DecoderMode
import com.chintan992.xplayer.ui.theme.BrandAccent

@Composable
fun TopBar(
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
    onWatchCastClick: () -> Unit,
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
                contentDescription = androidx.compose.ui.res.stringResource(com.chintan992.xplayer.R.string.player_back),
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
                            contentDescription = androidx.compose.ui.res.stringResource(com.chintan992.xplayer.R.string.player_subtitles),
                            tint = Color.White
                        )
                    }
                }
                
                if (hasAudioTracks) {
                    IconButton(onClick = onAudioClick) {
                         Icon(
                            imageVector = Icons.Outlined.Headphones,
                            contentDescription = androidx.compose.ui.res.stringResource(com.chintan992.xplayer.R.string.player_audio),
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
                        contentDescription = androidx.compose.ui.res.stringResource(com.chintan992.xplayer.R.string.player_speed),
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
        
        GoogleCastButton(
             modifier = Modifier.padding(end = 4.dp)
        )

        IconButton(onClick = onWatchCastClick) {
            Icon(
                imageVector = Icons.Outlined.Watch,
                contentDescription = androidx.compose.ui.res.stringResource(com.chintan992.xplayer.R.string.player_cast_watch),
                tint = Color.White
            )
        }

        IconButton(onClick = onSettingsToggle) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = androidx.compose.ui.res.stringResource(com.chintan992.xplayer.R.string.player_settings),
                tint = if (showInlineSettings) BrandAccent else Color.White
            )
        }
    }
}
