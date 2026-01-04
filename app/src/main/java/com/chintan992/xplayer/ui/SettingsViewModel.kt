package com.chintan992.xplayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chintan992.xplayer.PlayerPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val playerPreferencesRepository: PlayerPreferencesRepository
) : ViewModel() {

    val defaultPlayerType: StateFlow<String> = playerPreferencesRepository.defaultPlayerType
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "EXO"
        )

    fun updateDefaultPlayerType(type: String) {
        viewModelScope.launch {
            playerPreferencesRepository.updateDefaultPlayerType(type)
        }
    }
}
