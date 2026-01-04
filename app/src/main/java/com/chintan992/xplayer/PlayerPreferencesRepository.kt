package com.chintan992.xplayer

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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
    }

    val defaultPlayerType: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.DEFAULT_PLAYER_TYPE] ?: "EXO"
        }

    suspend fun updateDefaultPlayerType(type: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_PLAYER_TYPE] = type
        }
    }

    companion object {
        const val TAG = "PlayerPreferencesRepo"
    }
}
