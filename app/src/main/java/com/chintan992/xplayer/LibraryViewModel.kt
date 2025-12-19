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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
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
    val fieldVisibility: FieldVisibility = FieldVisibility()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LocalMediaRepository
) : ViewModel() {

    private val _viewMode = MutableStateFlow(ViewMode.FOLDERS)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _selectedFolder = MutableStateFlow<VideoFolder?>(null)
    val selectedFolder: StateFlow<VideoFolder?> = _selectedFolder.asStateFlow()

    private val _settings = MutableStateFlow(FolderViewSettings())
    val settings: StateFlow<FolderViewSettings> = _settings.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    // Raw videos from repository
    private val rawVideos = _selectedFolder.flatMapLatest { folder ->
        if (folder != null) {
            repository.getVideosByFolder(folder.path)
        } else {
            repository.getVideos()
        }
    }

    // Sorted videos based on settings
    val videos = combine(rawVideos, _settings) { videoList, settings ->
        val sorted = when (settings.sortBy) {
            SortBy.TITLE -> videoList.sortedBy { it.name.lowercase() }
            SortBy.DATE -> videoList.sortedBy { it.dateModified }
            SortBy.SIZE -> videoList.sortedBy { it.size }
            SortBy.DURATION -> videoList.sortedBy { it.duration }
        }
        if (settings.sortOrder == SortOrder.DESCENDING) sorted.reversed() else sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Raw folders from repository
    private val rawFolders = repository.getVideoFolders()

    // Sorted folders based on settings
    val folders = combine(rawFolders, _settings) { folderList, settings ->
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
        _settings.value = _settings.value.copy(layoutType = type)
    }

    fun setSortBy(sortBy: SortBy) {
        _settings.value = _settings.value.copy(sortBy = sortBy)
    }

    fun setSortOrder(order: SortOrder) {
        _settings.value = _settings.value.copy(sortOrder = order)
    }

    fun toggleFieldVisibility(field: String) {
        val current = _settings.value.fieldVisibility
        val updated = when (field) {
            "thumbnail" -> current.copy(thumbnail = !current.thumbnail)
            "duration" -> current.copy(duration = !current.duration)
            "fileExtension" -> current.copy(fileExtension = !current.fileExtension)
            "size" -> current.copy(size = !current.size)
            "date" -> current.copy(date = !current.date)
            "path" -> current.copy(path = !current.path)
            else -> current
        }
        _settings.value = _settings.value.copy(fieldVisibility = updated)
    }
}
