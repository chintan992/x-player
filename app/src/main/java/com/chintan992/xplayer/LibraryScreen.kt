package com.chintan992.xplayer

import com.chintan992.xplayer.ui.theme.BrandAccent

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.draw.clip
import android.os.Environment
import android.content.Intent
import android.provider.Settings
import com.google.accompanist.permissions.rememberPermissionState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight

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
                            when {
                                selectedFolder != null -> selectedFolder!!.name
                                viewMode == ViewMode.FOLDERS -> "Folders"
                                else -> "All Videos"
                            }
                        )
                    },
                    navigationIcon = {
                        if (selectedFolder != null) {
                            IconButton(onClick = { viewModel.clearSelectedFolder() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    actions = {
                        // Settings button
                        IconButton(onClick = { viewModel.showSettings() }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings"
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
                                        "Switch to Folder View" 
                                    else 
                                        "Switch to All Videos"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            when {
                viewMode == ViewMode.FOLDERS && selectedFolder == null -> {
                    // Show folder list (matching reference design)
                    if (folders.isEmpty()) {
                        EmptyState(
                            message = "No folders found",
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
                            message = if (selectedFolder != null) "No videos in this folder" else "No videos found",
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
                    title = { Text("Video Library") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folder thumbnail
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
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
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Folder info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${folder.videoCount} video${if (folder.videoCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Size badge
                    Text(
                        text = formatFileSize(folder.totalSize),
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                

            }
        }
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
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
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
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Duration badge
                    if (fieldVisibility.duration) {
                        Text(
                            text = formatDuration(video.duration),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    // Progress indicator
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
                }
            }
            
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (fieldVisibility.size) {
                    Text(
                        text = formatFileSize(video.size),
                        style = MaterialTheme.typography.bodySmall,
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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            if (fieldVisibility.thumbnail) {
                Box(
                    modifier = Modifier
                        .size(80.dp, 45.dp)
                        .clip(RoundedCornerShape(6.dp))
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
                        Text(
                            text = formatDuration(video.duration),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(2.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    // Progress indicator
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
                
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            // Video info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (fieldVisibility.size) {
                        Text(
                            text = formatFileSize(video.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (fieldVisibility.duration && !fieldVisibility.thumbnail) {
                        Text(
                            text = formatDuration(video.duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                

            }
        }
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
        Text(
            text = message,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Videos from your device will appear here",
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
        Text(
            text = "Permission Required",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Grant storage access to view your videos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
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
        title = "View Settings",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Layout & Sort Group
            Column {
                Text(
                    text = "Display",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Row 1: Layout + Sort Order
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Layout Toggle
                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        LayoutType.entries.forEach { type ->
                            val isSelected = settings.layoutType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) BrandAccent else Color.Transparent)
                                    .clickable { onLayoutTypeChange(type) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable {
                                onSortOrderChange(
                                    if (settings.sortOrder == SortOrder.ASCENDING) SortOrder.DESCENDING 
                                    else SortOrder.ASCENDING
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
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

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2: Sort Criteria (Scrollable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortBy.entries.forEach { sort ->
                        androidx.compose.material3.FilterChip(
                            selected = settings.sortBy == sort,
                            onClick = { onSortByChange(sort) },
                            label = { Text(sort.displayName) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandAccent.copy(alpha = 0.2f),
                                selectedLabelColor = BrandAccent,
                                labelColor = Color.White
                            ),
                            border = null
                        )
                    }
                }
            }

            // Fields Compact Grid
            Column {
                Text(
                    text = "Visible Fields",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val fields = listOf(
                        "Thumbnail" to settings.fieldVisibility.thumbnail,
                        "Duration" to settings.fieldVisibility.duration,
                        "Size" to settings.fieldVisibility.size,
                        "Path" to settings.fieldVisibility.path,
                        "Date" to settings.fieldVisibility.date,
                        "Extension" to settings.fieldVisibility.fileExtension
                    )
                    
                    fields.forEach { (label, isChecked) ->
                        val key = when(label) {
                            "Extension" -> "fileExtension"
                            else -> label.lowercase()
                        }
                        
                        androidx.compose.material3.FilterChip(
                            selected = isChecked,
                            onClick = { onFieldToggle(key) },
                            label = { Text(label) },
                            leadingIcon = if (isChecked) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandAccent.copy(alpha = 0.2f),
                                selectedLabelColor = BrandAccent,
                                labelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
            
            androidx.compose.material3.HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Hidden Folders
            val context = LocalContext.current
            var showPermissionRequest by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (settings.showHiddenFolders) {
                            onToggleHidden()
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                                showPermissionRequest = true
                            } else {
                                onToggleHidden()
                            }
                        }
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Show Hidden Folders", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                androidx.compose.material3.Switch(
                    checked = settings.showHiddenFolders,
                    onCheckedChange = { 
                        if (it) {
                             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                                showPermissionRequest = true
                            } else {
                                onToggleHidden()
                            }
                        } else {
                            onToggleHidden()
                        }
                    },
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BrandAccent
                    )
                )
            }
            
            if (showPermissionRequest) {
                 androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showPermissionRequest = false },
                    title = { Text("Permission Required") },
                    text = { Text("Hidden folders require full storage access.") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            showPermissionRequest = false
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                intent.data = android.net.Uri.parse("package:${context.packageName}")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                context.startActivity(intent)
                            }
                        }) { Text("Grant") }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { showPermissionRequest = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCheckedChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            colors = androidx.compose.material3.CheckboxDefaults.colors(
                checkedColor = BrandAccent,
                checkmarkColor = Color.White,
                uncheckedColor = Color.White.copy(alpha = 0.6f)
            )
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
