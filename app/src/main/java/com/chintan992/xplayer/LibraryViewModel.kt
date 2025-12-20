package com.chintan992.xplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // Raw videos from repository
    private val rawVideos = combine(_selectedFolder, settings.map { it.showHiddenFolders }.distinctUntilChanged()) { folder, showHidden ->
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
    private val rawFolders = settings.map { it.showHiddenFolders }
        .distinctUntilChanged()
        .flatMapLatest { showHidden ->
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
