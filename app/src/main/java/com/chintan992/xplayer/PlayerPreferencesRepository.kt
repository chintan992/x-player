package com.chintan992.xplayer

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class PlayerPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val DEFAULT_PLAYER_TYPE = stringPreferencesKey("default_player_type")
        val DEFAULT_ORIENTATION = booleanPreferencesKey("default_orientation")
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val DEFAULT_ASPECT_RATIO = stringPreferencesKey("default_aspect_ratio")
        val DEFAULT_DECODER = stringPreferencesKey("default_decoder")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val SEEK_DURATION = intPreferencesKey("seek_duration")
        val LONG_PRESS_SPEED = floatPreferencesKey("long_press_speed")
        val CONTROLS_TIMEOUT = intPreferencesKey("controls_timeout")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    }

    // Default values
    object Defaults {
        const val ORIENTATION_LANDSCAPE = true
        const val SPEED = 1.0f
        const val ASPECT_RATIO = "FIT"
        const val DECODER = "AUTO"
        const val AUTO_PLAY = true
        const val SEEK_DURATION_SECONDS = 10
        const val LONG_PRESS_SPEED = 2.0f
        const val CONTROLS_TIMEOUT_MS = 3000
        const val RESUME_PLAYBACK = true
        const val KEEP_SCREEN_ON = true
    }

    private fun <T> createPreferenceFlow(
        key: Preferences.Key<T>,
        defaultValue: T
    ): Flow<T> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[key] ?: defaultValue
        }

    // Flows for each preference
    val defaultPlayerType: Flow<String> = createPreferenceFlow(
        PreferencesKeys.DEFAULT_PLAYER_TYPE, "EXO"
    )

    val defaultOrientation: Flow<Boolean> = createPreferenceFlow(
        PreferencesKeys.DEFAULT_ORIENTATION, Defaults.ORIENTATION_LANDSCAPE
    )

    val defaultSpeed: Flow<Float> = createPreferenceFlow(
        PreferencesKeys.DEFAULT_SPEED, Defaults.SPEED
    )

    val defaultAspectRatio: Flow<String> = createPreferenceFlow(
        PreferencesKeys.DEFAULT_ASPECT_RATIO, Defaults.ASPECT_RATIO
    )

    val defaultDecoder: Flow<String> = createPreferenceFlow(
        PreferencesKeys.DEFAULT_DECODER, Defaults.DECODER
    )

    val autoPlayNext: Flow<Boolean> = createPreferenceFlow(
        PreferencesKeys.AUTO_PLAY_NEXT, Defaults.AUTO_PLAY
    )

    val seekDuration: Flow<Int> = createPreferenceFlow(
        PreferencesKeys.SEEK_DURATION, Defaults.SEEK_DURATION_SECONDS
    )

    val longPressSpeed: Flow<Float> = createPreferenceFlow(
        PreferencesKeys.LONG_PRESS_SPEED, Defaults.LONG_PRESS_SPEED
    )

    val controlsTimeout: Flow<Int> = createPreferenceFlow(
        PreferencesKeys.CONTROLS_TIMEOUT, Defaults.CONTROLS_TIMEOUT_MS
    )

    val resumePlayback: Flow<Boolean> = createPreferenceFlow(
        PreferencesKeys.RESUME_PLAYBACK, Defaults.RESUME_PLAYBACK
    )

    val keepScreenOn: Flow<Boolean> = createPreferenceFlow(
        PreferencesKeys.KEEP_SCREEN_ON, Defaults.KEEP_SCREEN_ON
    )

    // Update functions
    suspend fun updateDefaultPlayerType(type: String) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_PLAYER_TYPE] = type }
    }

    suspend fun updateDefaultOrientation(isLandscape: Boolean) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_ORIENTATION] = isLandscape }
    }

    suspend fun updateDefaultSpeed(speed: Float) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_SPEED] = speed }
    }

    suspend fun updateDefaultAspectRatio(aspectRatio: String) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_ASPECT_RATIO] = aspectRatio }
    }

    suspend fun updateDefaultDecoder(decoder: String) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_DECODER] = decoder }
    }

    suspend fun updateAutoPlayNext(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun updateSeekDuration(seconds: Int) {
        dataStore.edit { it[PreferencesKeys.SEEK_DURATION] = seconds }
    }

    suspend fun updateLongPressSpeed(speed: Float) {
        dataStore.edit { it[PreferencesKeys.LONG_PRESS_SPEED] = speed }
    }

    suspend fun updateControlsTimeout(timeoutMs: Int) {
        dataStore.edit { it[PreferencesKeys.CONTROLS_TIMEOUT] = timeoutMs }
    }

    suspend fun updateResumePlayback(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.RESUME_PLAYBACK] = enabled }
    }

    suspend fun updateKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.KEEP_SCREEN_ON] = enabled }
    }

    companion object {
        const val TAG = "PlayerPreferencesRepo"
    }
}
