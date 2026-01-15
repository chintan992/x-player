package com.chintan992.xplayer

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.compose.runtime.LaunchedEffect
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

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun LibraryScreen(
    onVideoClick: (VideoItem) -> Unit,
    onSettingsClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp)
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

        val isSearching by viewModel.isSearching.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()
        
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        
        // Dialog States
        var showDeleteDialog by remember { mutableStateOf(false) }
        var showRenameDialog by remember { mutableStateOf(false) }
        var showMoveDialog by remember { mutableStateOf(false) }
        var showCopyDialog by remember { mutableStateOf(false) }
        
        // ActivityResultLauncher for system delete permission dialog (Android 11+)
        val deletePermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.onDeletePermissionGranted()
            } else {
                viewModel.onDeletePermissionDenied()
            }
        }

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
        LaunchedEffect(viewModel.uiEvent) {
             viewModel.uiEvent.collect { message ->
                 Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
             }
        }
        
        // Handle File Events (permission requests, messages)
        LaunchedEffect(viewModel.fileEvent) {
            viewModel.fileEvent.collect { event ->
                when (event) {
                    is FileEvent.ShowMessage -> {
                        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    }
                    is FileEvent.RequestDeletePermission -> {
                        deletePermissionLauncher.launch(
                            IntentSenderRequest.Builder(event.intentSender).build()
                        )
                    }
                    is FileEvent.RequestAllFilesAccess -> {
                        // Show toast and open Settings for All Files Access
                        Toast.makeText(
                            context,
                            "Please enable 'All files access' to modify files",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                            ).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }

        // Handle back press
        BackHandler(enabled = isSelectionMode || selectedFolder != null || isSearching) {
            viewModel.onBackPressed()
        }

            val topBarHeight = 64.dp // Standard TopAppBar height
            val bottomBarHeight = if (isSelectionMode) 80.dp else 0.dp // Approximate height for SelectionBar
            
            val topInset = androidx.compose.foundation.layout.WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
            // We use the passed contentPadding for bottom (which includes NavBar + BottomNav), OR manual calculation if 0
            val parentBottomPadding = contentPadding.calculateBottomPadding()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // CONTENT (List)
                // We pass the full padding needed to clear the bars + system bars
                val listContentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = topInset + topBarHeight,
                    // If selection mode, ensure we clear the selection bar.
                    // If regular mode, clear the parent bottom padding (MainNavBar)
                    bottom = if (isSelectionMode) parentBottomPadding + bottomBarHeight else parentBottomPadding
                )
                
                when {
                    viewMode == ViewMode.FOLDERS && selectedFolder == null && !isSearching -> {
                        if (folders.isEmpty()) {
                            EmptyState(modifier = Modifier.padding(top = topInset + topBarHeight), message = stringResource(R.string.empty_folders))
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
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = listContentPadding
                            )
                        }
                    }
                    else -> {
                        // Video List (All videos, folder contents, or SEARCH RESULTS)
                        // If searching in folders mode and no folder selected, we show filtered folders?
                        // The ViewModel logic currently filters 'folders' if search query is present.
                        // So if we are in FOLDERS view mode and searching, we should show the filtered 'FolderList'
                        // UNLESS logic changes to search global videos?
                        // Current logic: 
                        // videos = rawVideos + filter
                        // folders = rawFolders + filter
                        // rawVideos depends on selectedFolder.
                        // if selectedFolder is NULL, rawVideos is ALL videos.
                        
                        // Let's refine the UI logic to match ViewModel flows:
                        
                        if (viewMode == ViewMode.FOLDERS && selectedFolder == null) {
                             // Showing Folders (Filtered or not)
                             if (folders.isEmpty()) {
                                 val msg = if (searchQuery.isNotEmpty()) "No folders found for \"$searchQuery\"" else stringResource(R.string.empty_folders)
                                 EmptyState(modifier = Modifier.padding(top = topInset + topBarHeight), message = msg)
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
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = listContentPadding
                                )
                             }
                        } else {
                            // Showing Videos (All Videos, Folder Content, or Search Results for videos)
                            if (videos.isEmpty()) {
                                 val msg = if (searchQuery.isNotEmpty()) "No videos found for \"$searchQuery\"" 
                                           else if (selectedFolder != null) stringResource(R.string.empty_videos_folder) 
                                           else stringResource(R.string.empty_videos)
                                           
                                 EmptyState(
                                    modifier = Modifier.padding(top = topInset + topBarHeight), 
                                    message = msg
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
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = listContentPadding
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
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = listContentPadding
                                    )
                                }
                            }
                        }
                    }
                }

                // TOP BAR (Overlay)
                // We wrap it in a separate Box to handle the background blur/color
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f)) // Translucent
                        // .blur(20.dp) // TODO: Check if blur is supported, sticking to alpha for safety/consistency for now
                ) {
                     if (isSearching) {
                         // SEARCH BAR
                         com.chintan992.xplayer.library.ui.SearchAppBar(
                             query = searchQuery,
                             onQueryChange = { viewModel.updateSearchQuery(it) },
                             onCloseClicked = { viewModel.exitSearchMode() }
                         )
                     } else {
                         androidx.compose.material3.TopAppBar(
                            title = {
                                 if (isSelectionMode) {
                                     Text(
                                         text = "${viewModel.getSelectedCount()} Selected",
                                         fontWeight = FontWeight.SemiBold
                                     )
                                 } else {
                                    Text(
                                        text = if (selectedFolder != null) selectedFolder!!.name else "XPlayer", // Brand Title or Folder Name
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 34.sp, // Increased size for bold header
                                        letterSpacing = (-1).sp // Tight letter spacing
                                    )
                                }
                            },
                            navigationIcon = {
                                if (isSelectionMode) {
                                     IconButton(onClick = { viewModel.exitSelectionMode() }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.content_desc_close_selection)
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
                                     // No actions here
                                } else {
                                    // Search Button
                                    IconButton(onClick = { viewModel.toggleSearch() }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = "Search"
                                        )
                                    }

                                    // Sort Button
                                    IconButton(onClick = { viewModel.showSettings() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Sort,
                                            contentDescription = "Sort"
                                        )
                                    }
                                    
                                    // Select Button
                                    IconButton(onClick = { viewModel.enterSelectionMode() }) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckBox,
                                            contentDescription = "Select"
                                        )
                                    }
                                    
                                    // Settings button
                                    IconButton(onClick = { onSettingsClick() }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Settings,
                                            contentDescription = stringResource(R.string.action_settings)
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = MaterialTheme.colorScheme.onBackground,
                                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                }

                // BOTTOM BAR (Overlay)
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = contentPadding.calculateBottomPadding())
                            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f))
                    ) {
                        com.chintan992.xplayer.library.ui.SelectionBar(
                            selectedCount = viewModel.getSelectedCount(),
                            onRename = { showRenameDialog = true },
                            onDelete = { showDeleteDialog = true },
                            onMove = { showMoveDialog = true },
                            onCopy = { showCopyDialog = true },
                            onSelectAll = { viewModel.selectAll() }
                        )
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
