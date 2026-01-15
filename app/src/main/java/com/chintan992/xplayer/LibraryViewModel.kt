package com.chintan992.xplayer

import android.content.IntentSender
import android.os.Build
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sealed class for events that require UI interaction
 */
sealed class FileEvent {
    data class ShowMessage(val message: String) : FileEvent()
    data class RequestDeletePermission(val intentSender: IntentSender) : FileEvent()
    object RequestAllFilesAccess : FileEvent()
}

enum class ViewMode {
    ALL_VIDEOS,
    FOLDERS
}

enum class LayoutType {
    LIST,
    GRID
}

enum class SortBy(val displayName: String) {
    TITLE("Title"),
    DATE("Date"),
    SIZE("Size"),
    DURATION("Length")
}

enum class SortOrder {
    ASCENDING,  // Oldest/A-Z
    DESCENDING  // Newest/Z-A
}

data class FieldVisibility(
    val thumbnail: Boolean = true,
    val duration: Boolean = true,
    val fileExtension: Boolean = false,
    val size: Boolean = true,
    val date: Boolean = false,
    val path: Boolean = true
)

data class FolderViewSettings(
    val layoutType: LayoutType = LayoutType.LIST,
    val sortBy: SortBy = SortBy.TITLE,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val fieldVisibility: FieldVisibility = FieldVisibility(),
    val showHiddenFolders: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LocalMediaRepository,
    private val playbackPositionManager: PlaybackPositionManager,
    private val preferencesRepository: LibraryPreferencesRepository
) : ViewModel() {

    private val _viewMode = MutableStateFlow(ViewMode.FOLDERS)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _selectedFolder = MutableStateFlow<VideoFolder?>(null)
    val selectedFolder: StateFlow<VideoFolder?> = _selectedFolder.asStateFlow()

    // Persistent settings from DataStore
    val settings: StateFlow<FolderViewSettings> = preferencesRepository.folderViewSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FolderViewSettings()
        )

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _playbackPositions = MutableStateFlow<Map<String, Pair<Long, Long>>>(emptyMap())
    val playbackPositions: StateFlow<Map<String, Pair<Long, Long>>> = _playbackPositions.asStateFlow()

    init {
        refreshPlaybackPositions()
    }

    fun refreshPlaybackPositions() {
        viewModelScope.launch {
            _playbackPositions.value = playbackPositionManager.getAllPositions()
        }
    }

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    // Refresh trigger
    private val _refreshTrigger = MutableStateFlow(0)

    fun refresh() {
        _refreshTrigger.value += 1
    }

    // Raw videos from repository
    private val rawVideos = combine(
        _selectedFolder,
        settings.map { it.showHiddenFolders }.distinctUntilChanged(),
        _refreshTrigger
    ) { folder, showHidden, _ ->
        Pair(folder, showHidden)
    }.flatMapLatest { (folder, showHidden) ->
        if (folder != null) {
            repository.getVideosByFolder(folder.path, showHidden)
        } else {
            repository.getVideos(showHidden)
        }
    }

    // Sorted videos based on settings & Search
    val videos = combine(rawVideos, settings, _searchQuery) { videoList, settings, query ->
        val filtered = if (query.isBlank()) {
            videoList
        } else {
            videoList.filter { it.name.contains(query, ignoreCase = true) }
        }

        val sorted = when (settings.sortBy) {
            SortBy.TITLE -> filtered.sortedBy { it.name.lowercase() }
            SortBy.DATE -> filtered.sortedBy { it.dateModified }
            SortBy.SIZE -> filtered.sortedBy { it.size }
            SortBy.DURATION -> filtered.sortedBy { it.duration }
        }
        if (settings.sortOrder == SortOrder.DESCENDING) sorted.reversed() else sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Raw folders from repository
    private val rawFolders = combine(
        settings.map { it.showHiddenFolders }.distinctUntilChanged(),
        _refreshTrigger
    ) { showHidden, _ ->
        showHidden
    }.flatMapLatest { showHidden ->
        repository.getVideoFolders(showHidden)
    }

    // Sorted folders based on settings & Search
    val folders = combine(rawFolders, settings, _searchQuery) { folderList, settings, query ->
        val filtered = if (query.isBlank()) {
            folderList
        } else {
            folderList.filter { it.name.contains(query, ignoreCase = true) }
        }

        val sorted = when (settings.sortBy) {
            SortBy.TITLE -> filtered.sortedBy { it.name.lowercase() }
            SortBy.DATE -> filtered // No date for folders yet
            SortBy.SIZE -> filtered.sortedBy { it.totalSize }
            SortBy.DURATION -> filtered // No duration for folders
        }
        if (settings.sortOrder == SortOrder.DESCENDING) sorted.reversed() else sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleViewMode() {
        if (_isSearching.value) {
            exitSearchMode()
        }
        _viewMode.value = when (_viewMode.value) {
            ViewMode.ALL_VIDEOS -> ViewMode.FOLDERS
            ViewMode.FOLDERS -> ViewMode.ALL_VIDEOS
        }
        if (_viewMode.value == ViewMode.ALL_VIDEOS) {
            _selectedFolder.value = null
        }
    }

    fun selectFolder(folder: VideoFolder) {
        if (_isSearching.value) {
            exitSearchMode()
        }
        _selectedFolder.value = folder
    }

    fun clearSelectedFolder() {
        _selectedFolder.value = null
    }

    fun onBackPressed(): Boolean {
        if (_isSearching.value) {
            exitSearchMode()
            return true
        } else if (isSelectionMode.value) {
            exitSelectionMode()
            return true
        } else if (_selectedFolder.value != null) {
            _selectedFolder.value = null
            return true
        } else {
            return false
        }
    }

    // Search Functions
    fun toggleSearch() {
        _isSearching.value = !_isSearching.value
        if (!_isSearching.value) {
            _searchQuery.value = ""
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun exitSearchMode() {
        _isSearching.value = false
        _searchQuery.value = ""
    }

    // Settings functions
    fun showSettings() {
        _showSettingsDialog.value = true
    }

    fun hideSettings() {
        _showSettingsDialog.value = false
    }

    fun setLayoutType(type: LayoutType) {
        viewModelScope.launch {
            preferencesRepository.updateLayoutType(type)
        }
    }

    fun setSortBy(sortBy: SortBy) {
        viewModelScope.launch {
            preferencesRepository.updateSortBy(sortBy)
        }
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch {
            preferencesRepository.updateSortOrder(order)
        }
    }

    fun toggleShowHiddenFolders() {
        viewModelScope.launch {
            preferencesRepository.updateShowHiddenFolders(!settings.value.showHiddenFolders)
        }
    }

    // Selection State
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode = _isSelectionMode.asStateFlow()

    private val _selectedVideos = MutableStateFlow<Set<Long>>(emptySet())
    val selectedVideos = _selectedVideos.asStateFlow()

    private val _selectedFolders = MutableStateFlow<Set<String>>(emptySet())
    val selectedFolders = _selectedFolders.asStateFlow()
    
    // UI Events (simple messages)
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()
    
    // File Events (may require permission flow)
    private val _fileEvent = MutableSharedFlow<FileEvent>()
    val fileEvent = _fileEvent.asSharedFlow()
    
    // Store pending delete for retry after permission
    private var pendingDeleteVideos: List<VideoItem> = emptyList()

    fun enterSelectionMode() {
        if (_isSearching.value) {
            exitSearchMode()
        }
        _isSelectionMode.value = true
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        clearSelection()
    }

    fun toggleVideoSelection(video: VideoItem) {
        if (!_isSelectionMode.value) enterSelectionMode()
        
        val current = _selectedVideos.value.toMutableSet()
        if (current.contains(video.id)) {
            current.remove(video.id)
        } else {
            current.add(video.id)
        }
        _selectedVideos.value = current
        
        if (current.isEmpty() && _selectedFolders.value.isEmpty()) {
            exitSelectionMode()
        }
    }

    fun toggleFolderSelection(folder: VideoFolder) {
        if (!_isSelectionMode.value) enterSelectionMode()

        val current = _selectedFolders.value.toMutableSet()
        if (current.contains(folder.path)) {
            current.remove(folder.path)
        } else {
            current.add(folder.path)
        }
        _selectedFolders.value = current
        
        if (current.isEmpty() && _selectedVideos.value.isEmpty()) {
            exitSelectionMode()
        }
    }

    fun selectAll() {
        if (_selectedFolder.value != null) {
            // Selecting videos in current folder
            val mappedIds = videos.value.map { it.id }.toSet()
            _selectedVideos.value = mappedIds
        } else if (_viewMode.value == ViewMode.FOLDERS) {
            // Selecting folders
            val mappedPaths = folders.value.map { it.path }.toSet()
            _selectedFolders.value = mappedPaths
        } else {
             // Selecting all videos
             val mappedIds = videos.value.map { it.id }.toSet()
             _selectedVideos.value = mappedIds
        }
    }

    fun clearSelection() {
        _selectedVideos.value = emptySet()
        _selectedFolders.value = emptySet()
    }
    
    /**
     * Check if the app has All Files Access permission (MANAGE_EXTERNAL_STORAGE)
     * Required for file modifications on Android R+
     */
    private fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Not needed on Android 10 and below
        }
    }
    
    // File Operations Logic
    fun deleteSelected() {
        viewModelScope.launch {
            // Check for All Files Access on Android R+
            if (!hasAllFilesAccess()) {
                _fileEvent.emit(FileEvent.RequestAllFilesAccess)
                return@launch
            }
            
            // Get videos to delete
            val videosToDelete = if (_selectedVideos.value.isNotEmpty()) {
                videos.value.filter { _selectedVideos.value.contains(it.id) }
            } else emptyList()
            
            // Get folders to delete
            val foldersToDelete = if (_selectedFolders.value.isNotEmpty()) {
                folders.value.filter { _selectedFolders.value.contains(it.path) }
            } else emptyList()
            
            var totalSuccess = 0
            
            // Handle videos with modern API
            if (videosToDelete.isNotEmpty()) {
                pendingDeleteVideos = videosToDelete
                when (val result = repository.deleteVideosModern(videosToDelete)) {
                    is FileOperationResult.Success -> {
                        totalSuccess += result.count
                        pendingDeleteVideos = emptyList()
                    }
                    is FileOperationResult.NeedsPermission -> {
                        // Emit event for UI to show system permission dialog
                        _fileEvent.emit(FileEvent.RequestDeletePermission(result.intentSender))
                        return@launch // Wait for user response
                    }
                    is FileOperationResult.Error -> {
                        _fileEvent.emit(FileEvent.ShowMessage("Delete failed: ${result.message}"))
                    }
                }
            }
            
            // Handle folders (legacy method, no scoped storage issues for folders)
            foldersToDelete.forEach { folder ->
                if (repository.deleteFolder(folder)) totalSuccess++
            }
            
            if (totalSuccess > 0) {
                _uiEvent.emit("Deleted $totalSuccess items")
                exitSelectionMode()
                refresh()
            } else if (videosToDelete.isEmpty() && foldersToDelete.isNotEmpty()) {
                _uiEvent.emit("Failed to delete folders")
            }
        }
    }
    
    /**
     * Called when user grants permission in system delete dialog
     */
    fun onDeletePermissionGranted() {
        viewModelScope.launch {
            // After permission granted, files are already deleted by system
            val count = pendingDeleteVideos.size
            pendingDeleteVideos = emptyList()
            _uiEvent.emit("Deleted $count items")
            exitSelectionMode()
            refresh()
        }
    }
    
    /**
     * Called when user denies permission in system delete dialog
     */
    fun onDeletePermissionDenied() {
        viewModelScope.launch {
            pendingDeleteVideos = emptyList()
            _uiEvent.emit("Delete cancelled")
        }
    }
    
    fun renameSelected(newName: String) {
        viewModelScope.launch {
            if (_selectedVideos.value.size == 1) {
                val video = videos.value.find { it.id == _selectedVideos.value.first() }
                if (video != null) {
                    if (repository.renameVideo(video, newName)) {
                        _uiEvent.emit("Renamed 1 item")
                        exitSelectionMode()
                        refresh()
                    } else {
                        _uiEvent.emit("Rename failed")
                    }
                }
            } else if (_selectedFolders.value.size == 1) {
                val folder = folders.value.find { it.path == _selectedFolders.value.first() }
                if (folder != null) {
                    if (repository.renameFolder(folder, newName)) {
                        _uiEvent.emit("Renamed 1 item")
                        exitSelectionMode()
                        refresh()
                    } else {
                        _uiEvent.emit("Rename failed")
                    }
                }
            }
        }
    }

    // Store pending move for retry after permission
    private var pendingMoveVideos: List<VideoItem> = emptyList()
    private var pendingMoveTargetFolder: VideoFolder? = null

    fun moveSelected(targetFolder: VideoFolder) {
        viewModelScope.launch {
            // Check for All Files Access on Android R+
            if (!hasAllFilesAccess()) {
                _fileEvent.emit(FileEvent.RequestAllFilesAccess)
                return@launch
            }
            
            val videosToMove = videos.value.filter { _selectedVideos.value.contains(it.id) }
            
            if (videosToMove.isEmpty()) {
                _uiEvent.emit("No videos selected")
                return@launch
            }
            
            pendingMoveVideos = videosToMove
            pendingMoveTargetFolder = targetFolder
            
            var successCount = 0
            var hasPermissionRequest = false
            
            for (video in videosToMove) {
                when (val result = repository.moveVideoModern(video, targetFolder.path)) {
                    is FileOperationResult.Success -> {
                        successCount += result.count
                    }
                    is FileOperationResult.NeedsPermission -> {
                        // Need user permission to delete original after copy
                        _fileEvent.emit(FileEvent.RequestDeletePermission(result.intentSender))
                        hasPermissionRequest = true
                        break // Wait for user to grant permission
                    }
                    is FileOperationResult.Error -> {
                        _fileEvent.emit(FileEvent.ShowMessage("Move failed: ${result.message}"))
                    }
                }
            }
            
            if (!hasPermissionRequest) {
                pendingMoveVideos = emptyList()
                pendingMoveTargetFolder = null
                
                if (successCount > 0) {
                    _uiEvent.emit("Moved $successCount items")
                    exitSelectionMode()
                    refresh()
                } else {
                    _uiEvent.emit("Move failed")
                }
            }
        }
    }
    
    /**
     * Called when user grants move/delete permission - continue the pending move
     */
    fun onMovePermissionGranted() {
        viewModelScope.launch {
            val count = pendingMoveVideos.size
            pendingMoveVideos = emptyList()
            pendingMoveTargetFolder = null
            _uiEvent.emit("Moved $count items")
            exitSelectionMode()
            refresh()
        }
    }
    
    fun copySelected(targetFolder: VideoFolder) {
        viewModelScope.launch {
            var count = 0
            val videosToCopy = videos.value.filter { _selectedVideos.value.contains(it.id) }
            videosToCopy.forEach { video ->
                if (repository.copyVideo(video, targetFolder.path)) count++
            }
            
            if (count > 0) {
                _uiEvent.emit("Copied $count items")
                exitSelectionMode()
                refresh()
            } else {
                _uiEvent.emit("Copy failed")
            }
        }
    }

    fun getSelectedCount(): Int {
        return _selectedVideos.value.size + _selectedFolders.value.size
    }

    fun toggleFieldVisibility(field: String) {
        viewModelScope.launch {
            val current = settings.value.fieldVisibility
            when (field) {
                "thumbnail" -> preferencesRepository.updateFieldVisibility(thumbnail = !current.thumbnail)
                "duration" -> preferencesRepository.updateFieldVisibility(duration = !current.duration)
                "fileExtension" -> preferencesRepository.updateFieldVisibility(fileExtension = !current.fileExtension)
                "size" -> preferencesRepository.updateFieldVisibility(size = !current.size)
                "path" -> preferencesRepository.updateFieldVisibility(path = !current.path)
                "date" -> preferencesRepository.updateFieldVisibility(date = !current.date)
                else -> {}
            }
        }
    }
}
