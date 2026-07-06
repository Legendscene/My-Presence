package com.kyrx.mypresence.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_AUTO_START_ENABLED = booleanPreferencesKey("auto_start_enabled")
        private val KEY_PRESENCE_NAME = stringPreferencesKey("presence_name")
        private val KEY_PRESENCE_DETAILS = stringPreferencesKey("presence_details")
        private val KEY_PRESENCE_STATE = stringPreferencesKey("presence_state")
        private val KEY_PRESENCE_TYPE = intPreferencesKey("presence_type")
    }

    override val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    override val accessToken: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_ACCESS_TOKEN]
    }

    override val refreshToken: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_REFRESH_TOKEN]
    }

    override val notificationsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    override val autoStartEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_START_ENABLED] ?: false
    }

    override val presenceName: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_PRESENCE_NAME] ?: "My Presence"
    }

    override val presenceDetails: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_PRESENCE_DETAILS] ?: ""
    }

    override val presenceState: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_PRESENCE_STATE] ?: ""
    }

    override val presenceType: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_PRESENCE_TYPE] ?: 0
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    override suspend fun saveUserInfo(userId: String, username: String) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USERNAME] = username
        }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    override suspend fun setAutoStartEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_START_ENABLED] = enabled
        }
    }

    override suspend fun savePresenceConfig(name: String, details: String, state: String, type: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_PRESENCE_NAME] = name
            prefs[KEY_PRESENCE_DETAILS] = details
            prefs[KEY_PRESENCE_STATE] = state
            prefs[KEY_PRESENCE_TYPE] = type
        }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
