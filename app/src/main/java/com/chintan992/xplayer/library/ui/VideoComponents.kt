package com.chintan992.xplayer.library.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.chintan992.xplayer.VideoItem
import com.chintan992.xplayer.FieldVisibility
import com.chintan992.xplayer.ui.theme.BrandAccent
import com.chintan992.xplayer.ui.theme.Dimens

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun VideoGrid(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onVideoLongClick: (VideoItem) -> Unit,
    isSelectionMode: Boolean,
    selectedVideoIds: Set<Long>,
    fieldVisibility: FieldVisibility,
    playbackPositions: Map<String, Pair<Long, Long>>,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.padding(Dimens.SpacingMedium),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
    ) {
        items(videos) { video ->
            val positionInfo = playbackPositions[video.uri.toString()]
            val isSelected = selectedVideoIds.contains(video.id)
            VideoGridItem(
                video = video,
                fieldVisibility = fieldVisibility,
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                playbackPosition = positionInfo,
                onClick = { onVideoClick(video) },
                onLongClick = { onVideoLongClick(video) },
                animatedVisibilityScope = animatedVisibilityScope,
                sharedTransitionScope = sharedTransitionScope
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun VideoList(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onVideoLongClick: (VideoItem) -> Unit,
    isSelectionMode: Boolean,
    selectedVideoIds: Set<Long>,
    fieldVisibility: FieldVisibility,
    playbackPositions: Map<String, Pair<Long, Long>>,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(Dimens.SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
    ) {
        items(videos) { video ->
            val positionInfo = playbackPositions[video.uri.toString()]
            val isSelected = selectedVideoIds.contains(video.id)
            VideoListItem(
                video = video,
                fieldVisibility = fieldVisibility,
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                playbackPosition = positionInfo,
                onClick = { onVideoClick(video) },
                onLongClick = { onVideoLongClick(video) },
                animatedVisibilityScope = animatedVisibilityScope,
                sharedTransitionScope = sharedTransitionScope
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun VideoGridItem(
    video: VideoItem,
    fieldVisibility: FieldVisibility,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    playbackPosition: Pair<Long, Long>?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else Dimens.CardElevation),
        shape = RoundedCornerShape(Dimens.CornerLarge),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, BrandAccent) else null
    ) {
        Column {
            // Thumbnail container
            Box {
                 if (fieldVisibility.thumbnail) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    ) {
                        with(sharedTransitionScope) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(video.uri)
                                    .videoFrameMillis(1000)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = video.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState(key = "video-${video.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        // Gradient Overlay
                         Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                        startY = 100f
                                    )
                                )
                        )
                        
                        // Duration badge
                        if (fieldVisibility.duration) {
                            Text(
                                text = formatDuration(video.duration),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(Dimens.SpacingSmall),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Progress indicator
                        playbackPosition?.let { (pos, _) ->
                            if (pos > 0) {
                                val progress = pos.toFloat() / video.duration.coerceAtLeast(1)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(Dimens.ProgressBarHeight)
                                        .align(Alignment.BottomStart)
                                        .background(Color.White.copy(alpha = 0.3f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress)
                                            .fillMaxHeight()
                                            .background(BrandAccent)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Selection Checkbox Overlay
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(if (isSelected) Color.Black.copy(alpha = 0.3f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                             Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = androidx.compose.ui.res.stringResource(com.chintan992.xplayer.R.string.content_desc_selected),
                                tint = BrandAccent,
                                modifier = Modifier.size(32.dp).background(Color.White, androidx.compose.foundation.shape.CircleShape)
                            )
                        } else {
                             // Empty circle for unselected hint
                             Icon(
                                imageVector = Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = androidx.compose.ui.res.stringResource(com.chintan992.xplayer.R.string.content_desc_unselected),
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.padding(Dimens.SpacingMedium)) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (fieldVisibility.size) {
                    Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                    Text(
                        text = formatFileSize(video.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun VideoListItem(
    video: VideoItem,
    fieldVisibility: FieldVisibility,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    playbackPosition: Pair<Long, Long>?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope
) {
    val context = LocalContext.current
    
    androidx.compose.material3.ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.CornerMedium))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        headlineContent = {
             Text(
                text = video.name,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
                 if (fieldVisibility.size) {
                    Text(
                        text = formatFileSize(video.size),
                    )
                }
                if (fieldVisibility.duration && !fieldVisibility.thumbnail) {
                    Text(
                        text = formatDuration(video.duration),
                    )
                }
            }
        },
        leadingContent = {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(width = Dimens.VideoListItemTitleWidth, height = Dimens.VideoListItemThumbnailHeight)
                    .clip(RoundedCornerShape(Dimens.CornerSmall)),
                 contentAlignment = Alignment.Center
            ) {
                if (fieldVisibility.thumbnail) {
                    with(sharedTransitionScope) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(video.uri)
                                .videoFrameMillis(1000)
                                .crossfade(true)
                                .build(),
                            contentDescription = video.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "video-${video.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    // Duration badge on thumbnail
                    if (fieldVisibility.duration) {
                         Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                        )
                        Text(
                            text = formatDuration(video.duration),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(2.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                    
                    // Progress indicator
                    playbackPosition?.let { (pos, _) ->
                        if (pos > 0) {
                            val progress = pos.toFloat() / video.duration.coerceAtLeast(1)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(Dimens.ProgressBarHeightSmall)
                                    .align(Alignment.BottomStart)
                                    .background(Color.White.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .background(BrandAccent)
                                )
                            }
                        }
                    }
                }
                
                // Selection Overlay
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
                                contentDescription = androidx.compose.ui.res.stringResource(com.chintan992.xplayer.R.string.content_desc_selected),
                                tint = BrandAccent,
                                modifier = Modifier.size(24.dp).background(Color.White, androidx.compose.foundation.shape.CircleShape)
                            )
                        } else {
                             Icon(
                                imageVector = Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = androidx.compose.ui.res.stringResource(com.chintan992.xplayer.R.string.content_desc_unselected),
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        trailingContent = {
             if (!isSelectionMode) {
                 androidx.compose.material3.IconButton(onClick = { /* TODO: Show options menu */ }) {
                     Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = androidx.compose.ui.res.stringResource(com.chintan992.xplayer.R.string.content_desc_more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Dimens.IconMedium)
                    )
                 }
            }
        },
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent,
            headlineColor = MaterialTheme.colorScheme.onBackground,
            supportingColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
