package com.chintan992.xplayer.library.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
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
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp)
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Dimens.SpacingMedium,
            end = Dimens.SpacingMedium,
            top = contentPadding.calculateTopPadding() + Dimens.SpacingSmall,
            bottom = contentPadding.calculateBottomPadding() + Dimens.SpacingSmall
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
    ) {
        items(folders, key = { it.path }) { folder ->
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
    
    // Animated selection effects - only show border when selected
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) BrandAccent else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "folderBorderColor"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) BrandAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "folderContainerColor"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
             )
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp, 
                    color = borderColor, 
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Folder Thumbnail / Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Selection Overlay
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) BrandAccent else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // Text Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${folder.videoCount} videos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Optional: Chevron or meta info
             if (folder.totalSize > 0 && fieldVisibility.size) {
                 // Maybe show size? Wireframe doesn't explicitly show it, just title/count
             }
        }
    }
}
