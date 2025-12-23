package com.chintan992.xplayer.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PrivacyPolicyRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val POLICY_ACCEPTED = booleanPreferencesKey("privacy_policy_accepted")
        val ACCEPTED_TIMESTAMP = longPreferencesKey("privacy_policy_accepted_timestamp")
    }

    val isPolicyAccepted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.POLICY_ACCEPTED] ?: false
    }

    suspend fun acceptPolicy() {
        dataStore.edit { preferences ->
            preferences[Keys.POLICY_ACCEPTED] = true
            preferences[Keys.ACCEPTED_TIMESTAMP] = System.currentTimeMillis()
        }
    }
}
