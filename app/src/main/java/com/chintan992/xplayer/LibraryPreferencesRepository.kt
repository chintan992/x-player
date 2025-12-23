package com.chintan992.xplayer

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class LibraryPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val LAYOUT_TYPE = stringPreferencesKey("layout_type")
        val SORT_BY = stringPreferencesKey("sort_by")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val SHOW_HIDDEN_FOLDERS = booleanPreferencesKey("show_hidden_folders")
        
        // Field Visibility
        val FIELD_THUMBNAIL = booleanPreferencesKey("field_thumbnail")
        val FIELD_DURATION = booleanPreferencesKey("field_duration")
        val FIELD_SIZE = booleanPreferencesKey("field_size")
        val FIELD_PATH = booleanPreferencesKey("field_path")
        val FIELD_DATE = booleanPreferencesKey("field_date")
        val FIELD_EXTENSION = booleanPreferencesKey("field_extension")
    }

    val folderViewSettings: Flow<FolderViewSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Error reading preferences.", exception)
                }
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapPreferencesToSettings(preferences)
        }

    suspend fun updateLayoutType(layoutType: LayoutType) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAYOUT_TYPE] = layoutType.name
        }
    }

    suspend fun updateSortBy(sortBy: SortBy) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_BY] = sortBy.name
        }
    }

    suspend fun updateSortOrder(sortOrder: SortOrder) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun updateShowHiddenFolders(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_HIDDEN_FOLDERS] = show
        }
    }

    suspend fun updateFieldVisibility(
        thumbnail: Boolean? = null,
        duration: Boolean? = null,
        size: Boolean? = null,
        path: Boolean? = null,
        date: Boolean? = null,
        fileExtension: Boolean? = null
    ) {
        dataStore.edit { preferences ->
            thumbnail?.let { preferences[PreferencesKeys.FIELD_THUMBNAIL] = it }
            duration?.let { preferences[PreferencesKeys.FIELD_DURATION] = it }
            size?.let { preferences[PreferencesKeys.FIELD_SIZE] = it }
            path?.let { preferences[PreferencesKeys.FIELD_PATH] = it }
            date?.let { preferences[PreferencesKeys.FIELD_DATE] = it }
            fileExtension?.let { preferences[PreferencesKeys.FIELD_EXTENSION] = it }
        }
    }

    private fun mapPreferencesToSettings(preferences: Preferences): FolderViewSettings {
        val layoutType = LayoutType.valueOf(
            preferences[PreferencesKeys.LAYOUT_TYPE] ?: LayoutType.LIST.name
        )
        val sortBy = SortBy.valueOf(
            preferences[PreferencesKeys.SORT_BY] ?: SortBy.TITLE.name
        )
        val sortOrder = SortOrder.valueOf(
            preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.ASCENDING.name
        )
        val showHiddenFolders = preferences[PreferencesKeys.SHOW_HIDDEN_FOLDERS] ?: false

        val fieldVisibility = FieldVisibility(
            thumbnail = preferences[PreferencesKeys.FIELD_THUMBNAIL] ?: true,
            duration = preferences[PreferencesKeys.FIELD_DURATION] ?: true,
            size = preferences[PreferencesKeys.FIELD_SIZE] ?: true,
            path = preferences[PreferencesKeys.FIELD_PATH] ?: false,
            date = preferences[PreferencesKeys.FIELD_DATE] ?: true,
            fileExtension = preferences[PreferencesKeys.FIELD_EXTENSION] ?: false
        )

        return FolderViewSettings(
            layoutType = layoutType,
            sortBy = sortBy,
            sortOrder = sortOrder,
            fieldVisibility = fieldVisibility,
            showHiddenFolders = showHiddenFolders
        )
    }
    
    companion object {
        const val TAG = "LibraryPreferencesRepo"
    }
}
