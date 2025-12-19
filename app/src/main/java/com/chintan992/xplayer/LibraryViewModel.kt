package com.chintan992.xplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class ViewMode {
    ALL_VIDEOS,
    FOLDERS
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LocalMediaRepository
) : ViewModel() {

    private val _viewMode = MutableStateFlow(ViewMode.ALL_VIDEOS)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _selectedFolder = MutableStateFlow<VideoFolder?>(null)
    val selectedFolder: StateFlow<VideoFolder?> = _selectedFolder.asStateFlow()

    val videos = _selectedFolder.flatMapLatest { folder ->
        if (folder != null) {
            repository.getVideosByFolder(folder.path)
        } else {
            repository.getVideos()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders = repository.getVideoFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            ViewMode.ALL_VIDEOS -> ViewMode.FOLDERS
            ViewMode.FOLDERS -> ViewMode.ALL_VIDEOS
        }
        // Clear selected folder when switching to all videos
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
            true // Handled
        } else {
            false // Not handled
        }
    }
}
