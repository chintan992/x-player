package com.chintan992.xplayer.library.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

@Composable
fun AutoScrollHandler(
    listState: androidx.compose.foundation.lazy.LazyListState,
    items: List<VideoItem>,
    targetId: String?,
    onScrollConsumed: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(targetId, items) {
        if (targetId != null && items.isNotEmpty()) {
            val index = items.indexOfFirst { it.id.toString() == targetId }
            if (index != -1) {
                listState.scrollToItem(index)
                onScrollConsumed()
            }
        }
    }
}

@Composable
fun AutoScrollHandler(
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    items: List<VideoItem>,
    targetId: String?,
    onScrollConsumed: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(targetId, items) {
        if (targetId != null && items.isNotEmpty()) {
            val index = items.indexOfFirst { it.id.toString() == targetId }
            if (index != -1) {
                gridState.scrollToItem(index)
                onScrollConsumed()
            }
        }
    }
}

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
    scrollToVideoId: String? = null,
    onScrollConsumed: () -> Unit = {},
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp)
) {
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    
    // Auto-scroll effect
    AutoScrollHandler(gridState, videos, scrollToVideoId, onScrollConsumed)

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Dimens.SpacingMedium,
            end = Dimens.SpacingMedium,
            top = contentPadding.calculateTopPadding() + Dimens.SpacingMedium,
            bottom = contentPadding.calculateBottomPadding() + Dimens.SpacingMedium
        ),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
    ) {
        items(videos, key = { it.id }) { video ->
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
                sharedTransitionScope = sharedTransitionScope,
                modifier = Modifier.animateItem(
                    fadeInSpec = tween(300),
                    fadeOutSpec = tween(200)
                )
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
    scrollToVideoId: String? = null,
    onScrollConsumed: () -> Unit = {},
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp)
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Auto-scroll effect
    AutoScrollHandler(listState, videos, scrollToVideoId, onScrollConsumed)

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Dimens.SpacingMedium,
            end = Dimens.SpacingMedium,
            top = contentPadding.calculateTopPadding() + Dimens.SpacingMedium,
            bottom = contentPadding.calculateBottomPadding() + Dimens.SpacingMedium
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
    ) {
        items(videos, key = { it.id }) { video ->
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
                sharedTransitionScope = sharedTransitionScope,
                modifier = Modifier.animateItem(
                    fadeInSpec = tween(300),
                    fadeOutSpec = tween(200)
                )
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
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Animated selection
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "videoScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        // Thumbnail container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp)) // Rounded corners for thumbnail
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
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
                            rememberSharedContentState(key = "video-${video.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Subtle Gradient for text readability if we placed text inside, 
            // but for this design we keep text outside, so this is just for the duration badge contrast or aesthetics
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)),
                            startY = 0.7f
                        )
                    )
            )
            
            // Duration Badge
            if (fieldVisibility.duration) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatDuration(video.duration),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
            
            // Progress Indicator
            playbackPosition?.let { (pos, _) ->
                if (pos > 0) {
                    val progress = pos.toFloat() / video.duration.coerceAtLeast(1)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
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

            // Selection Checkmark Overlay
            if (isSelectionMode) {
                 Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isSelected) Color.Black.copy(alpha = 0.5f) else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                         Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = BrandAccent,
                            modifier = Modifier.size(32.dp).background(Color.White, CircleShape).padding(2.dp)
                        )
                    } else {
                         Icon(
                            imageVector = Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Text Info (Outside Card)
        Text(
            text = video.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        if (fieldVisibility.size) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${formatFileSize(video.size)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Animated selection
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent, 
        label = "listItemBackground"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
         // Thumbnail
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
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
                                rememberSharedContentState(key = "video-${video.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            ),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Duration
                if (fieldVisibility.duration) {
                     Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatDuration(video.duration),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Progress
                playbackPosition?.let { (pos, _) ->
                    if (pos > 0) {
                        val progress = pos.toFloat() / video.duration.coerceAtLeast(1)
                         Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
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
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) BrandAccent else Color.White,
                        modifier = Modifier.size(24.dp).background(if (isSelected) Color.White else Color.Transparent, CircleShape).clip(CircleShape)
                    )
                }
            }
        }
        
        // Text Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatFileSize(video.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // More Video Options
        if (!isSelectionMode) {
             androidx.compose.material3.IconButton(
                 onClick = { /* TODO: Show options menu */ },
                 modifier = Modifier.size(Dimens.IconMedium)
             ) {
                 Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
             }
        }
    }
}
