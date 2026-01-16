package com.chintan992.xplayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chintan992.xplayer.PlayerPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val playerPreferencesRepository: PlayerPreferencesRepository,
    private val libraryPreferencesRepository: com.chintan992.xplayer.LibraryPreferencesRepository
) : ViewModel() {

    val defaultPlayerType: StateFlow<String> = playerPreferencesRepository.defaultPlayerType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "EXO")

    val defaultOrientation: StateFlow<Boolean> = playerPreferencesRepository.defaultOrientation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 
            PlayerPreferencesRepository.Defaults.ORIENTATION_LANDSCAPE)

    val defaultSpeed: StateFlow<Float> = playerPreferencesRepository.defaultSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 
            PlayerPreferencesRepository.Defaults.SPEED)

    val defaultAspectRatio: StateFlow<String> = playerPreferencesRepository.defaultAspectRatio
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 
            PlayerPreferencesRepository.Defaults.ASPECT_RATIO)

    val defaultDecoder: StateFlow<String> = playerPreferencesRepository.defaultDecoder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 
            PlayerPreferencesRepository.Defaults.DECODER)

    val autoPlayNext: StateFlow<Boolean> = playerPreferencesRepository.autoPlayNext
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 
            PlayerPreferencesRepository.Defaults.AUTO_PLAY)

    val seekDuration: StateFlow<Int> = playerPreferencesRepository.seekDuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 
            PlayerPreferencesRepository.Defaults.SEEK_DURATION_SECONDS)

    val longPressSpeed: StateFlow<Float> = playerPreferencesRepository.longPressSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 
            PlayerPreferencesRepository.Defaults.LONG_PRESS_SPEED)

    val controlsTimeout: StateFlow<Int> = playerPreferencesRepository.controlsTimeout
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 
            PlayerPreferencesRepository.Defaults.CONTROLS_TIMEOUT_MS)

    val resumePlayback: StateFlow<Boolean> = playerPreferencesRepository.resumePlayback
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 
            PlayerPreferencesRepository.Defaults.RESUME_PLAYBACK)

    val keepScreenOn: StateFlow<Boolean> = playerPreferencesRepository.keepScreenOn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 
            PlayerPreferencesRepository.Defaults.KEEP_SCREEN_ON)

    // Library Settings
    val autoScrollToLastPlayed: StateFlow<Boolean> = libraryPreferencesRepository.folderViewSettings
        .map { it.autoScrollToLastPlayed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun updateDefaultPlayerType(type: String) {
        viewModelScope.launch { playerPreferencesRepository.updateDefaultPlayerType(type) }
    }

    fun updateDefaultOrientation(isLandscape: Boolean) {
        viewModelScope.launch { playerPreferencesRepository.updateDefaultOrientation(isLandscape) }
    }

    fun updateDefaultSpeed(speed: Float) {
        viewModelScope.launch { playerPreferencesRepository.updateDefaultSpeed(speed) }
    }

    fun updateDefaultAspectRatio(aspectRatio: String) {
        viewModelScope.launch { playerPreferencesRepository.updateDefaultAspectRatio(aspectRatio) }
    }

    fun updateDefaultDecoder(decoder: String) {
        viewModelScope.launch { playerPreferencesRepository.updateDefaultDecoder(decoder) }
    }

    fun updateAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch { playerPreferencesRepository.updateAutoPlayNext(enabled) }
    }

    fun updateSeekDuration(seconds: Int) {
        viewModelScope.launch { playerPreferencesRepository.updateSeekDuration(seconds) }
    }

    fun updateLongPressSpeed(speed: Float) {
        viewModelScope.launch { playerPreferencesRepository.updateLongPressSpeed(speed) }
    }

    fun updateControlsTimeout(timeoutMs: Int) {
        viewModelScope.launch { playerPreferencesRepository.updateControlsTimeout(timeoutMs) }
    }

    fun updateResumePlayback(enabled: Boolean) {
        viewModelScope.launch { playerPreferencesRepository.updateResumePlayback(enabled) }
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { playerPreferencesRepository.updateKeepScreenOn(enabled) }
    }

    fun updateAutoScrollToLastPlayed(enabled: Boolean) {
        viewModelScope.launch { libraryPreferencesRepository.updateAutoScrollToLastPlayed(enabled) }
    }
}

