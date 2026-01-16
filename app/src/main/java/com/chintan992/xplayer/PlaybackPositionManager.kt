package com.chintan992.xplayer

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.playbackDataStore: DataStore<Preferences> by preferencesDataStore(name = "playback_positions")

@Singleton
class PlaybackPositionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Save the playback position for a video
     * @param videoId Unique identifier for the video (use content URI path or ID)
     * @param position Playback position in milliseconds
     * @param duration Total video duration in milliseconds
     */
    suspend fun savePosition(videoId: String, position: Long, duration: Long) {
        // Only save if position is significant (more than 5 seconds and less than 95% of video)
        val progressPercent = if (duration > 0) (position.toFloat() / duration) * 100 else 0f
        
        if (position > 5000 && progressPercent < 95f) {
            context.playbackDataStore.edit { preferences ->
                preferences[longPreferencesKey("pos_$videoId")] = position
                preferences[longPreferencesKey("dur_$videoId")] = duration
            }
        } else if (progressPercent >= 95f) {
            // Video completed - remove saved position
            clearPosition(videoId)
        }
    }
    
    /**
     * Get saved playback position for a video
     * @return Position in milliseconds, or 0 if not found
     */
    suspend fun getPosition(videoId: String): Long {
        return context.playbackDataStore.data.map { preferences ->
            preferences[longPreferencesKey("pos_$videoId")] ?: 0L
        }.first()
    }
    
    /**
     * Get saved duration for a video
     * @return Duration in milliseconds, or 0 if not found
     */
    suspend fun getDuration(videoId: String): Long {
        return context.playbackDataStore.data.map { preferences ->
            preferences[longPreferencesKey("dur_$videoId")] ?: 0L
        }.first()
    }
    
    /**
     * Clear saved position for a video
     */
    suspend fun clearPosition(videoId: String) {
        context.playbackDataStore.edit { preferences ->
            preferences.remove(longPreferencesKey("pos_$videoId"))
            preferences.remove(longPreferencesKey("dur_$videoId"))
        }
    }
    
    /**
     * Get all saved positions (for displaying resume indicators)
     */
    suspend fun getAllPositions(): Map<String, Pair<Long, Long>> {
        return context.playbackDataStore.data.map { preferences ->
            val result = mutableMapOf<String, Pair<Long, Long>>()
            preferences.asMap().forEach { (key, value) ->
                if (key.name.startsWith("pos_")) {
                    val videoId = key.name.removePrefix("pos_")
                    val position = value as? Long ?: 0L
                    val duration = preferences[longPreferencesKey("dur_$videoId")] ?: 0L
                    result[videoId] = Pair(position, duration)
                }
            }
            result
        }.first()
    }

    /**
     * Save the last played video ID for a specific folder
     */
    suspend fun saveLastPlayedVideo(folderPath: String, videoId: String) {
        context.playbackDataStore.edit { preferences ->
            preferences[stringPreferencesKey("last_played_$folderPath")] = videoId
        }
    }

    /**
     * Get the last played video ID for a specific folder
     */
    suspend fun getLastPlayedVideo(folderPath: String): String? {
        return context.playbackDataStore.data.map { preferences: Preferences ->
            preferences[stringPreferencesKey("last_played_$folderPath")]
        }.first()
    }
}
