package com.kyrx.mypresence.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kyrx.mypresence.domain.model.AppPresenceConfig
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_AUTO_START_ENABLED = booleanPreferencesKey("auto_start_enabled")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_PRESENCE_NAME = stringPreferencesKey("presence_name")
        private val KEY_PRESENCE_DETAILS = stringPreferencesKey("presence_details")
        private val KEY_PRESENCE_STATE = stringPreferencesKey("presence_state")
        private val KEY_PRESENCE_TYPE = intPreferencesKey("presence_type")
        private val KEY_ENABLED_APPS = stringPreferencesKey("enabled_apps")
        private val KEY_KEEP_ONLINE_24_7 = booleanPreferencesKey("keep_online_24_7")
        private val KEY_APP_PRESENCE_CONFIGS = stringPreferencesKey("app_presence_configs")
    }

    override val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    override val notificationsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    override val autoStartEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_START_ENABLED] ?: false
    }

    override val isDarkMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DARK_MODE] ?: true
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

    override val enabledApps: Flow<Set<String>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_ENABLED_APPS] ?: ""
        if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    override val keepOnline24_7: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_KEEP_ONLINE_24_7] ?: false
    }

    override suspend fun setKeepOnline24_7(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_KEEP_ONLINE_24_7] = enabled }
    }

    override val appPresenceConfigs: Flow<List<AppPresenceConfig>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_APP_PRESENCE_CONFIGS] ?: "[]"
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            AppPresenceConfig(
                packageName = obj.getString("packageName"),
                name = obj.optString("name", ""),
                details = obj.optString("details", ""),
                state = obj.optString("state", ""),
                activityType = obj.optInt("activityType", -1),
                privacySafe = obj.optBoolean("privacySafe", true)
            )
        }
    }

    override suspend fun saveAppPresenceConfig(config: AppPresenceConfig) {
        dataStore.edit { prefs ->
            val raw = prefs[KEY_APP_PRESENCE_CONFIGS] ?: "[]"
            val arr = JSONArray(raw)
            var found = false
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("packageName") == config.packageName) {
                    obj.put("name", config.name)
                    obj.put("details", config.details)
                    obj.put("state", config.state)
                    obj.put("activityType", config.activityType)
                    obj.put("privacySafe", config.privacySafe)
                    found = true
                    break
                }
            }
            if (!found) {
                arr.put(JSONObject().apply {
                    put("packageName", config.packageName)
                    put("name", config.name)
                    put("details", config.details)
                    put("state", config.state)
                    put("activityType", config.activityType)
                    put("privacySafe", config.privacySafe)
                })
            }
            prefs[KEY_APP_PRESENCE_CONFIGS] = arr.toString()
        }
    }

    override suspend fun removeAppPresenceConfig(packageName: String) {
        dataStore.edit { prefs ->
            val raw = prefs[KEY_APP_PRESENCE_CONFIGS] ?: "[]"
            val arr = JSONArray(raw)
            val keep = JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("packageName") != packageName) {
                    keep.put(obj)
                }
            }
            prefs[KEY_APP_PRESENCE_CONFIGS] = keep.toString()
        }
    }

    override suspend fun setEnabledApps(packages: Set<String>) {
        dataStore.edit { prefs ->
            prefs[KEY_ENABLED_APPS] = packages.joinToString(",")
        }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    override suspend fun setAutoStartEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_AUTO_START_ENABLED] = enabled }
    }

    override suspend fun setIsDarkMode(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_DARK_MODE] = enabled }
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
