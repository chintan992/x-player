package com.chintan992.xplayer.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chintan992.xplayer.R
import com.chintan992.xplayer.ui.theme.Dimens

@Composable
fun SelectionBar(
    selectedCount: Int,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = Dimens.SpacingSmall),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectionActionItem(
            icon = Icons.Default.CheckCircle,
            label = "All",
            onClick = onSelectAll
        )
        
        SelectionActionItem(
            icon = Icons.Outlined.DriveFileMove,
            label = stringResource(R.string.action_move),
            onClick = onMove
        )
        
        SelectionActionItem(
            icon = Icons.Outlined.ContentCopy,
            label = stringResource(R.string.action_copy),
            onClick = onCopy
        )

        SelectionActionItem(
            icon = Icons.Outlined.Edit,
            label = stringResource(R.string.action_rename),
            onClick = onRename,
            enabled = selectedCount == 1,
            alpha = if (selectedCount == 1) 1f else 0.3f
        )
        
        SelectionActionItem(
            icon = Icons.Outlined.Delete,
            label = stringResource(R.string.action_delete),
            onClick = onDelete
        )
    }
}

@Composable
fun SelectionActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    alpha: Float = 1f
) {
    Column(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(Dimens.SpacingSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
