package com.chintan992.xplayer.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CenterControls(
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
                contentDescription = androidx.compose.ui.res.stringResource(com.chintan992.xplayer.R.string.player_rewind_10),
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        FilledTonalIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) 
                    androidx.compose.ui.res.stringResource(com.chintan992.xplayer.R.string.player_pause) 
                else 
                    androidx.compose.ui.res.stringResource(com.chintan992.xplayer.R.string.player_play),
                modifier = Modifier.size(40.dp)
            )
        }

        IconButton(onClick = onSeekForward) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = androidx.compose.ui.res.stringResource(com.chintan992.xplayer.R.string.player_forward_10),
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
