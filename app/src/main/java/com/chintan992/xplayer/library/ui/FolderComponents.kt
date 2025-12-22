package com.chintan992.xplayer.library.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.chintan992.xplayer.VideoFolder
import com.chintan992.xplayer.FieldVisibility
import com.chintan992.xplayer.ui.theme.BrandAccent
import com.chintan992.xplayer.ui.theme.Dimens

@Composable
fun FolderList(
    folders: List<VideoFolder>,
    onFolderClick: (VideoFolder) -> Unit,
    onFolderLongClick: (VideoFolder) -> Unit,
    isSelectionMode: Boolean,
    selectedFolders: Set<String>,
    fieldVisibility: FieldVisibility,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = Dimens.SpacingMedium, vertical = Dimens.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
    ) {
        items(folders) { folder ->
            val isSelected = selectedFolders.contains(folder.path)
            FolderListItem(
                folder = folder,
                fieldVisibility = fieldVisibility,
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                onClick = { onFolderClick(folder) },
                onLongClick = { onFolderLongClick(folder) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderListItem(
    folder: VideoFolder,
    fieldVisibility: FieldVisibility,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Transparent Card for cleaner "Cinema" List look
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent
        ),
        shape = RoundedCornerShape(Dimens.CornerMedium),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, BrandAccent) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folder thumbnail
            Box(
                modifier = Modifier
                    .size(Dimens.FolderThumbnailSize)
                    .clip(RoundedCornerShape(Dimens.CornerMedium))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (folder.thumbnailUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(folder.thumbnailUri)
                            .videoFrameMillis(1000)
                            .crossfade(true)
                            .build(),
                        contentDescription = folder.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconLarge),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Selection Indicator Overlay
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = BrandAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                             Icon(
                                imageVector = Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = "Unselected",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(Dimens.SpacingLarge))
            
            // Folder info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
                ) {
                    Text(
                        text = "${folder.videoCount} video${if (folder.videoCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Size badge
                    if (folder.totalSize > 0) {
                        Text(
                            text = formatFileSize(folder.totalSize),
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(Dimens.CornerSmall)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Chevron for affordance (Hide in selection mode)
            if (!isSelectionMode) {
                 Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        // Divider
        HorizontalDivider(
            modifier = Modifier.padding(start = 84.dp), // Intentional indent
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
        )
    }
}
