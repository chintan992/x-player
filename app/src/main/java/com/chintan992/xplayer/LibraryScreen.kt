package com.chintan992.xplayer

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.chintan992.xplayer.library.ui.EmptyState
import com.chintan992.xplayer.library.ui.FolderList
import com.chintan992.xplayer.library.ui.FolderViewSettingsDialog
import com.chintan992.xplayer.library.ui.PermissionRequest
import com.chintan992.xplayer.library.ui.SelectionBar
import com.chintan992.xplayer.library.ui.DeleteDialog
import com.chintan992.xplayer.library.ui.RenameDialog
import com.chintan992.xplayer.library.ui.FolderPickerDialog
import com.chintan992.xplayer.library.ui.VideoGrid
import com.chintan992.xplayer.library.ui.VideoList
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.asSharedFlow

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun LibraryScreen(
    onVideoClick: (VideoItem) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope
) {
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
        
        // Selection State
        val isSelectionMode by viewModel.isSelectionMode.collectAsState()
        val selectedVideos by viewModel.selectedVideos.collectAsState()
        val selectedFolders by viewModel.selectedFolders.collectAsState()
        
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        
        // Dialog States
        var showDeleteDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        var showRenameDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        var showMoveDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        var showCopyDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

        // Refresh playback positions when screen resumes
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshPlaybackPositions()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
        
        // Handle UI Events (Toasts)
        androidx.compose.runtime.LaunchedEffect(viewModel.uiEvent) {
             viewModel.uiEvent.collect { message ->
                 android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
             }
        }

        // Handle back press
        BackHandler(enabled = isSelectionMode || selectedFolder != null) {
            if (isSelectionMode) {
                viewModel.exitSelectionMode()
            } else {
                viewModel.clearSelectedFolder()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                         if (isSelectionMode) {
                             Text(
                                 text = "${viewModel.getSelectedCount()} Selected",
                                 fontWeight = FontWeight.SemiBold
                             )
                         } else {
                            Text(
                                text = when {
                                    selectedFolder != null -> selectedFolder!!.name
                                    viewMode == ViewMode.FOLDERS -> stringResource(R.string.title_folders)
                                    else -> stringResource(R.string.title_all_videos)
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    navigationIcon = {
                        if (isSelectionMode) {
                             IconButton(onClick = { viewModel.exitSelectionMode() }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close"
                                )
                            }
                        } else if (selectedFolder != null) {
                            IconButton(onClick = { viewModel.clearSelectedFolder() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        }
                    },
                    actions = {
                        if (isSelectionMode) {
                             // No actions here, maybe Select All if not in bottom bar?
                             // Leaving empty for cleaner top bar as bottom bar has actions
                        } else {
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
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (isSelectionMode) {
                    com.chintan992.xplayer.library.ui.SelectionBar(
                        selectedCount = viewModel.getSelectedCount(),
                        onRename = { showRenameDialog = true },
                        onDelete = { showDeleteDialog = true },
                        onMove = { showMoveDialog = true },
                        onCopy = { showCopyDialog = true },
                        onSelectAll = { viewModel.selectAll() }
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            when {
                viewMode == ViewMode.FOLDERS && selectedFolder == null -> {
                    if (folders.isEmpty()) {
                        EmptyState(modifier = Modifier.padding(paddingValues), message = stringResource(R.string.empty_folders))
                    } else {
                        FolderList(
                            folders = folders,
                            onFolderClick = { 
                                if (isSelectionMode) viewModel.toggleFolderSelection(it) 
                                else viewModel.selectFolder(it) 
                            },
                            onFolderLongClick = { viewModel.toggleFolderSelection(it) },
                            isSelectionMode = isSelectionMode,
                            selectedFolders = selectedFolders,
                            fieldVisibility = settings.fieldVisibility,
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }
                else -> {
                    if (videos.isEmpty()) {
                         EmptyState(
                            modifier = Modifier.padding(paddingValues), 
                            message = if (selectedFolder != null) stringResource(R.string.empty_videos_folder) else stringResource(R.string.empty_videos)
                        )
                    } else {
                        val handleVideoClick: (VideoItem) -> Unit = { video ->
                            if (isSelectionMode) {
                                viewModel.toggleVideoSelection(video)
                            } else {
                                val index = videos.indexOf(video)
                                if (index != -1) {
                                    PlaylistManager.setPlaylist(videos, index)
                                }
                                onVideoClick(video)
                            }
                        }

                        when (settings.layoutType) {
                            LayoutType.GRID -> VideoGrid(
                                videos = videos,
                                onVideoClick = handleVideoClick,
                                onVideoLongClick = { viewModel.toggleVideoSelection(it) },
                                isSelectionMode = isSelectionMode,
                                selectedVideoIds = selectedVideos,
                                fieldVisibility = settings.fieldVisibility,
                                playbackPositions = playbackPositions,
                                animatedVisibilityScope = animatedVisibilityScope,
                                sharedTransitionScope = sharedTransitionScope,
                                modifier = Modifier.padding(paddingValues)
                            )
                            LayoutType.LIST -> VideoList(
                                videos = videos,
                                onVideoClick = handleVideoClick,
                                onVideoLongClick = { viewModel.toggleVideoSelection(it) },
                                isSelectionMode = isSelectionMode,
                                selectedVideoIds = selectedVideos,
                                fieldVisibility = settings.fieldVisibility,
                                playbackPositions = playbackPositions,
                                animatedVisibilityScope = animatedVisibilityScope,
                                sharedTransitionScope = sharedTransitionScope,
                                modifier = Modifier.padding(paddingValues)
                            )
                        }
                    }
                }
            }
        }

        // Dialogs
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
        
        if (showDeleteDialog) {
            com.chintan992.xplayer.library.ui.DeleteDialog(
                count = viewModel.getSelectedCount(),
                onConfirm = {
                    viewModel.deleteSelected()
                    showDeleteDialog = false
                },
                onDismiss = { showDeleteDialog = false }
            )
        }

        if (showRenameDialog) {
            // Determine initial name
            val initialName = if (selectedVideos.isNotEmpty()) {
                videos.find { it.id == selectedVideos.first() }?.name ?: ""
            } else if (selectedFolders.isNotEmpty()) {
                folders.find { it.path == selectedFolders.first() }?.name ?: ""
            } else ""

            com.chintan992.xplayer.library.ui.RenameDialog(
                initialName = initialName,
                onConfirm = { newName ->
                    viewModel.renameSelected(newName)
                    showRenameDialog = false
                },
                onDismiss = { showRenameDialog = false }
            )
        }

        if (showMoveDialog || showCopyDialog) {
             // Reuse folder list for picking. 
             // Ideally we want to show ALL folders except selected ones if they are folders?
             // For simplicity, showing all available folders.
             com.chintan992.xplayer.library.ui.FolderPickerDialog(
                folders = folders,
                onFolderSelected = { targetFolder ->
                    if (showMoveDialog) viewModel.moveSelected(targetFolder)
                    else if (showCopyDialog) viewModel.copySelected(targetFolder)
                    
                    showMoveDialog = false
                    showCopyDialog = false
                },
                onDismiss = {
                    showMoveDialog = false
                    showCopyDialog = false
                }
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
