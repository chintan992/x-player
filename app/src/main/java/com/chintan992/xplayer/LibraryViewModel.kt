package com.chintan992.xplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    // Sorted videos based on settings
    val videos = combine(rawVideos, settings) { videoList, settings ->
        val sorted = when (settings.sortBy) {
            SortBy.TITLE -> videoList.sortedBy { it.name.lowercase() }
            SortBy.DATE -> videoList.sortedBy { it.dateModified }
            SortBy.SIZE -> videoList.sortedBy { it.size }
            SortBy.DURATION -> videoList.sortedBy { it.duration }
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

    // Sorted folders based on settings
    val folders = combine(rawFolders, settings) { folderList, settings ->
        val sorted = when (settings.sortBy) {
            SortBy.TITLE -> folderList.sortedBy { it.name.lowercase() }
            SortBy.DATE -> folderList // No date for folders yet
            SortBy.SIZE -> folderList.sortedBy { it.totalSize }
            SortBy.DURATION -> folderList // No duration for folders
        }
        if (settings.sortOrder == SortOrder.DESCENDING) sorted.reversed() else sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            ViewMode.ALL_VIDEOS -> ViewMode.FOLDERS
            ViewMode.FOLDERS -> ViewMode.ALL_VIDEOS
        }
        if (_viewMode.value == ViewMode.ALL_VIDEOS) {
            _selectedFolder.value = null
        }
    }

    fun selectFolder(folder: VideoFolder) {
        _selectedFolder.value = folder
    }

    fun clearSelectedFolder() {
        _selectedFolder.value = null
    }

    fun onBackPressed(): Boolean {
        return if (_selectedFolder.value != null) {
            _selectedFolder.value = null
            true
        } else {
            false
        }
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
    
    // UI Events
    private val _uiEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun enterSelectionMode() {
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
    
    // File Operations Logic
    fun deleteSelected() {
        viewModelScope.launch {
            var successCount = 0
            val specifiedFolder = _selectedFolder.value
            
            // Delete Videos
            if (_selectedVideos.value.isNotEmpty()) {
                val videosToDelete = videos.value.filter { _selectedVideos.value.contains(it.id) }
                videosToDelete.forEach { video ->
                    if (repository.deleteVideo(video)) successCount++
                }
            }
            
            // Delete Folders
            if (_selectedFolders.value.isNotEmpty()) {
                val foldersToDelete = folders.value.filter { _selectedFolders.value.contains(it.path) }
                foldersToDelete.forEach { folder ->
                    if (repository.deleteFolder(folder)) successCount++
                }
            }
            
            if (successCount > 0) {
                _uiEvent.emit("Deleted $successCount items")
                exitSelectionMode()
                refresh()
            } else {
                _uiEvent.emit("Failed to delete items")
            }
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

    fun moveSelected(targetFolder: VideoFolder) {
        viewModelScope.launch {
            var count = 0
            val videosToMove = videos.value.filter { _selectedVideos.value.contains(it.id) }
            videosToMove.forEach { video ->
                if (repository.moveVideo(video, targetFolder.path)) count++
            }
            
            if (count > 0) {
                _uiEvent.emit("Moved $count items")
                exitSelectionMode()
                refresh()
            } else {
                _uiEvent.emit("Move failed")
            }
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
