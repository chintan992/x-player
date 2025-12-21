package com.chintan992.xplayer

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.chintan992.xplayer.ui.theme.BrandAccent
import com.chintan992.xplayer.ui.theme.Dimens
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onVideoClick: (VideoItem) -> Unit) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(permission)

    if (permissionState.status.isGranted) {
        val viewModel: LibraryViewModel = hiltViewModel()
        val viewMode by viewModel.viewMode.collectAsState()
        val selectedFolder by viewModel.selectedFolder.collectAsState()
        val videos by viewModel.videos.collectAsState()
        val folders by viewModel.folders.collectAsState()
        val settings by viewModel.settings.collectAsState()
        val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
        val playbackPositions by viewModel.playbackPositions.collectAsState()
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

        // Refresh playback positions when screen resumes
        androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshPlaybackPositions()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        // Handle back press when in folder view
        BackHandler(enabled = selectedFolder != null) {
            viewModel.clearSelectedFolder()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when {
                                selectedFolder != null -> selectedFolder!!.name
                                viewMode == ViewMode.FOLDERS -> stringResource(R.string.title_folders)
                                else -> stringResource(R.string.title_all_videos)
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        if (selectedFolder != null) {
                            IconButton(onClick = { viewModel.clearSelectedFolder() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        }
                    },
                    actions = {
                        // Settings button
                        IconButton(onClick = { viewModel.showSettings() }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.action_settings)
                            )
                        }
                        if (selectedFolder == null) {
                            IconButton(onClick = { viewModel.toggleViewMode() }) {
                                Icon(
                                    imageVector = if (viewMode == ViewMode.ALL_VIDEOS) 
                                        Icons.Outlined.Folder 
                                    else 
                                        Icons.Outlined.VideoLibrary,
                                    contentDescription = if (viewMode == ViewMode.ALL_VIDEOS) 
                                        stringResource(R.string.action_switch_folder_view) 
                                    else 
                                        stringResource(R.string.action_switch_all_videos)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        // Use Surface color for a cleaner, unified look (Cinema feel)
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background // Ensure dark background usage
        ) { paddingValues ->
            when {
                viewMode == ViewMode.FOLDERS && selectedFolder == null -> {
                    // Show folder list (matching reference design)
                    if (folders.isEmpty()) {
                        EmptyState(
                            message = stringResource(R.string.empty_folders),
                            modifier = Modifier.padding(paddingValues)
                        )
                    } else {
                        FolderList(
                            folders = folders,
                            onFolderClick = { viewModel.selectFolder(it) },
                            fieldVisibility = settings.fieldVisibility,
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }
                else -> {
                    // Show videos (all videos or videos in selected folder)
                    if (videos.isEmpty()) {
                        EmptyState(
                            message = if (selectedFolder != null) stringResource(R.string.empty_videos_folder) else stringResource(R.string.empty_videos),
                            modifier = Modifier.padding(paddingValues)
                        )
                    } else {
                        val handleVideoClick: (VideoItem) -> Unit = { video ->
                            val index = videos.indexOf(video)
                            if (index != -1) {
                                PlaylistManager.setPlaylist(videos, index)
                            }
                            onVideoClick(video)
                        }

                        when (settings.layoutType) {
                            LayoutType.GRID -> VideoGrid(
                                videos = videos,
                                onVideoClick = handleVideoClick,
                                fieldVisibility = settings.fieldVisibility,
                                playbackPositions = playbackPositions,
                                modifier = Modifier.padding(paddingValues)
                            )
                            LayoutType.LIST -> VideoList(
                                videos = videos,
                                onVideoClick = handleVideoClick,
                                fieldVisibility = settings.fieldVisibility,
                                playbackPositions = playbackPositions,
                                modifier = Modifier.padding(paddingValues)
                            )
                        }
                    }
                }
            }
        }

        // Settings Dialog
        if (showSettingsDialog) {
            FolderViewSettingsDialog(
                settings = settings,
                onLayoutTypeChange = { viewModel.setLayoutType(it) },
                onSortByChange = { viewModel.setSortBy(it) },
                onSortOrderChange = { viewModel.setSortOrder(it) },
                onFieldToggle = { viewModel.toggleFieldVisibility(it) },
                onToggleHidden = { viewModel.toggleShowHiddenFolders() },
                onDismiss = { viewModel.hideSettings() }
            )
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_video_library)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { paddingValues ->
            PermissionRequest(
                onRequestPermission = { permissionState.launchPermissionRequest() },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun FolderList(
    folders: List<VideoFolder>,
    onFolderClick: (VideoFolder) -> Unit,
    fieldVisibility: FieldVisibility,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = Dimens.SpacingMedium, vertical = Dimens.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
    ) {
        items(folders) { folder ->
            FolderListItem(folder = folder, fieldVisibility = fieldVisibility, onClick = { onFolderClick(folder) })
        }
    }
}

@Composable
private fun FolderListItem(
    folder: VideoFolder,
    fieldVisibility: FieldVisibility,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Transparent Card for cleaner "Cinema" List look
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(Dimens.CornerMedium)
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
            
            // Chevron for affordance
             Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        // Divider
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(start = 84.dp), // Intentional indent
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
        )
    }
}

@Composable
private fun VideoGrid(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    fieldVisibility: FieldVisibility,
    playbackPositions: Map<String, Pair<Long, Long>>,
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
            VideoGridItem(
                video = video,
                fieldVisibility = fieldVisibility,
                playbackPosition = positionInfo,
                onClick = { onVideoClick(video) }
            )
        }
    }
}

@Composable
private fun VideoList(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    fieldVisibility: FieldVisibility,
    playbackPositions: Map<String, Pair<Long, Long>>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(Dimens.SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
    ) {
        items(videos) { video ->
            val positionInfo = playbackPositions[video.uri.toString()]
            VideoListItem(
                video = video,
                fieldVisibility = fieldVisibility,
                playbackPosition = positionInfo,
                onClick = { onVideoClick(video) }
            )
        }
    }
}

@Composable
private fun VideoGridItem(
    video: VideoItem,
    fieldVisibility: FieldVisibility,
    playbackPosition: Pair<Long, Long>?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevation),
        shape = RoundedCornerShape(Dimens.CornerLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            if (fieldVisibility.thumbnail) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(video.uri)
                            .videoFrameMillis(1000)
                            .crossfade(true)
                            .build(),
                        contentDescription = video.name,
                        modifier = Modifier
                            .fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Gradient Overlay for text readability
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

@Composable
private fun VideoListItem(
    video: VideoItem,
    fieldVisibility: FieldVisibility,
    playbackPosition: Pair<Long, Long>?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Cleaner List Item
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.CornerMedium))
            .clickable(onClick = onClick)
            .padding(Dimens.SpacingMedium), // Internal padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        if (fieldVisibility.thumbnail) {
            Box(
                modifier = Modifier
                    .size(width = Dimens.VideoListItemTitleWidth, height = Dimens.VideoListItemThumbnailHeight)
                    .clip(RoundedCornerShape(Dimens.CornerSmall))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(video.uri)
                        .videoFrameMillis(1000)
                        .crossfade(true)
                        .build(),
                    contentDescription = video.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
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
            
            Spacer(modifier = Modifier.width(Dimens.SpacingLarge))
        }
        
        // Video info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
            ) {
                if (fieldVisibility.size) {
                    Text(
                        text = formatFileSize(video.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (fieldVisibility.duration && !fieldVisibility.thumbnail) {
                    Text(
                        text = formatDuration(video.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // More options icon placeholder
         Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "More",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimens.IconMedium)
        )
    }
}

@Composable
private fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.VideoLibrary, // Engaging icon
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
        Text(
            text = message,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.empty_state_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun PermissionRequest(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(Dimens.SpacingLarge))
        Text(
            text = stringResource(R.string.permission_required),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.permission_rationale),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Dimens.SpacingMedium, bottom = Dimens.SpacingSection)
        )
        Button(onClick = onRequestPermission) {
            Text(stringResource(R.string.permission_grant))
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.2f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderViewSettingsDialog(
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
