package com.chintan992.xplayer.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chintan992.xplayer.FolderViewSettings
import com.chintan992.xplayer.LayoutType
import com.chintan992.xplayer.R
import com.chintan992.xplayer.CustomBaseDialog
import com.chintan992.xplayer.SortBy
import com.chintan992.xplayer.SortOrder
import com.chintan992.xplayer.ui.theme.BrandAccent
import com.chintan992.xplayer.ui.theme.Dimens
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.width

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FolderViewSettingsDialog(
    settings: FolderViewSettings,
    onLayoutTypeChange: (LayoutType) -> Unit,
    onSortByChange: (SortBy) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onFieldToggle: (String) -> Unit,
    onToggleHidden: () -> Unit,
    onDismiss: () -> Unit
) {
    CustomBaseDialog(
        title = stringResource(R.string.dialog_view_settings),
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingDouble) // More airy
        ) {
            // Layout & Sort Group
            Column {
                Text(
                    text = stringResource(R.string.settings_display),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
                
                // Row 1: Layout + Sort Order
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Layout Toggle
                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(Dimens.CornerMedium))
                            .padding(Dimens.SpacingSmall)
                    ) {
                        LayoutType.entries.forEach { type ->
                            val isSelected = settings.layoutType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(Dimens.CornerSmall))
                                    .background(if (isSelected) BrandAccent else Color.Transparent)
                                    .clickable { onLayoutTypeChange(type) }
                                    .padding(horizontal = Dimens.SpacingLarge, vertical = 6.dp)
                            ) {
                                Text(
                                    text = type.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Sort Order Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.CornerMedium))
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable {
                                onSortOrderChange(
                                    if (settings.sortOrder == SortOrder.ASCENDING) SortOrder.DESCENDING 
                                    else SortOrder.ASCENDING
                                )
                            }
                            .padding(horizontal = Dimens.SpacingLarge, vertical = 8.dp)
                    ) {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             Text(
                                 text = if (settings.sortOrder == SortOrder.ASCENDING) "Ascending \u2191" else "Descending \u2193",
                                 style = MaterialTheme.typography.labelMedium,
                                 color = Color.White
                             )
                         }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

                // Row 2: Sort Criteria (Scrollable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
                ) {
                    SortBy.entries.forEach { sort ->
                        androidx.compose.material3.FilterChip(
                            selected = settings.sortBy == sort,
                            onClick = { onSortByChange(sort) },
                            label = { Text(sort.displayName) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandAccent.copy(alpha = 0.2f),
                                selectedLabelColor = BrandAccent,
                                labelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.05f)
                            ),
                            border = null
                        )
                    }
                }
            }

            // Visible Fields
            Column {
                Text(
                    text = "Visible Info",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
                ) {
                    // Helper composable for toggle chips
                    val ToggleChip = @Composable { label: String, isChecked: Boolean, onToggle: () -> Unit ->
                        androidx.compose.material3.FilterChip(
                            selected = isChecked,
                            onClick = onToggle,
                            label = { Text(label) },
                            leadingIcon = if (isChecked) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(Dimens.IconSmall)) }
                            } else null,
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandAccent.copy(alpha = 0.2f),
                                selectedLabelColor = BrandAccent,
                                labelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.05f)
                            ),
                            border = null
                        )
                    }

                    ToggleChip("Thumbnail", settings.fieldVisibility.thumbnail) { onFieldToggle("thumbnail") }
                    ToggleChip("Size", settings.fieldVisibility.size) { onFieldToggle("size") }
                    ToggleChip("Duration", settings.fieldVisibility.duration) { onFieldToggle("duration") }
                    ToggleChip("Date", settings.fieldVisibility.date) { onFieldToggle("date") }
                }
            }
            
            // Hidden Folders
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show Hidden Folders",
                         style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    androidx.compose.material3.Switch(
                        checked = settings.showHiddenFolders,
                        onCheckedChange = { onToggleHidden() },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = BrandAccent,
                            checkedTrackColor = BrandAccent.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun RenameDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialName) }
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_rename)) },
         text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotEmpty() && text != initialName
            ) {
                Text(stringResource(R.string.action_rename))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun DeleteDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_delete)) },
        text = { Text(stringResource(R.string.delete_confirmation, count)) },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun FolderPickerDialog(
    folders: List<com.chintan992.xplayer.VideoFolder>,
    onFolderSelected: (com.chintan992.xplayer.VideoFolder) -> Unit,
    onDismiss: () -> Unit
) {
    CustomBaseDialog(
        title = "Select Destination",
        onDismiss = onDismiss
    ) {
        if (folders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No other folders found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
            ) {
                items(folders.size) { index ->
                    val folder = folders[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.CornerMedium))
                            .clickable { onFolderSelected(folder) }
                            .padding(horizontal = Dimens.SpacingSmall, vertical = Dimens.SpacingSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon Container
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = BrandAccent.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(Dimens.CornerSmall)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Rounded.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = BrandAccent
                            )
                        }

                        Spacer(modifier = Modifier.width(Dimens.SpacingMedium))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${folder.videoCount} videos \u2022 ${folder.path}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
