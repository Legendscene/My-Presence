package com.kyrx.mypresence.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kyrx.mypresence.domain.model.AppPresenceConfig
import com.kyrx.mypresence.domain.model.CustomRpcPreset
import com.kyrx.mypresence.domain.model.PresenceData
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
        private val KEY_PRESENCE_LARGE_IMAGE = stringPreferencesKey("presence_large_image")
        private val KEY_PRESENCE_LARGE_TEXT = stringPreferencesKey("presence_large_text")
        private val KEY_PRESENCE_SMALL_IMAGE = stringPreferencesKey("presence_small_image")
        private val KEY_PRESENCE_SMALL_TEXT = stringPreferencesKey("presence_small_text")
        private val KEY_PRESENCE_START_TIMESTAMP = stringPreferencesKey("presence_start_timestamp")
        private val KEY_PRESENCE_END_TIMESTAMP = stringPreferencesKey("presence_end_timestamp")
        private val KEY_PRESENCE_STATUS = stringPreferencesKey("presence_status")
        private val KEY_PRESENCE_BUTTON1_LABEL = stringPreferencesKey("presence_button1_label")
        private val KEY_PRESENCE_BUTTON1_URL = stringPreferencesKey("presence_button1_url")
        private val KEY_PRESENCE_BUTTON2_LABEL = stringPreferencesKey("presence_button2_label")
        private val KEY_PRESENCE_BUTTON2_URL = stringPreferencesKey("presence_button2_url")
        private val KEY_PRESENCE_PARTY_SIZE = stringPreferencesKey("presence_party_size")
        private val KEY_PRESENCE_PARTY_MAX = stringPreferencesKey("presence_party_max")
        private val KEY_PRESENCE_PLATFORM = stringPreferencesKey("presence_platform")
        private val KEY_PRESENCE_STREAM_URL = stringPreferencesKey("presence_stream_url")
        private val KEY_ENABLED_APPS = stringPreferencesKey("enabled_apps")
        private val KEY_KEEP_ONLINE_24_7 = booleanPreferencesKey("keep_online_24_7")
        private val KEY_APP_PRESENCE_CONFIGS = stringPreferencesKey("app_presence_configs")
        private val KEY_PRESENCE_ENABLED = booleanPreferencesKey("presence_enabled")
        private val KEY_ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        private val KEY_USER_TOKEN = stringPreferencesKey("user_token")
        private val KEY_CUSTOM_RPC_PRESETS = stringPreferencesKey("custom_rpc_presets")
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

    override val customPresence: Flow<PresenceData> = dataStore.data.map { prefs ->
        PresenceData(
            name = prefs[KEY_PRESENCE_NAME] ?: "My Presence",
            details = prefs[KEY_PRESENCE_DETAILS] ?: "",
            state = prefs[KEY_PRESENCE_STATE] ?: "",
            type = prefs[KEY_PRESENCE_TYPE] ?: 0,
            largeImage = prefs[KEY_PRESENCE_LARGE_IMAGE] ?: "",
            largeText = prefs[KEY_PRESENCE_LARGE_TEXT] ?: "",
            smallImage = prefs[KEY_PRESENCE_SMALL_IMAGE] ?: "",
            smallText = prefs[KEY_PRESENCE_SMALL_TEXT] ?: "",
            startTimestamp = prefs[KEY_PRESENCE_START_TIMESTAMP]?.toLongOrNull(),
            endTimestamp = prefs[KEY_PRESENCE_END_TIMESTAMP]?.toLongOrNull(),
            status = prefs[KEY_PRESENCE_STATUS] ?: "online",
            enabled = prefs[KEY_PRESENCE_ENABLED] ?: false,
            button1Label = prefs[KEY_PRESENCE_BUTTON1_LABEL] ?: "",
            button1Url = prefs[KEY_PRESENCE_BUTTON1_URL] ?: "",
            button2Label = prefs[KEY_PRESENCE_BUTTON2_LABEL] ?: "",
            button2Url = prefs[KEY_PRESENCE_BUTTON2_URL] ?: "",
            partySize = prefs[KEY_PRESENCE_PARTY_SIZE]?.toIntOrNull(),
            partyMax = prefs[KEY_PRESENCE_PARTY_MAX]?.toIntOrNull(),
            platform = prefs[KEY_PRESENCE_PLATFORM] ?: "android",
            streamUrl = prefs[KEY_PRESENCE_STREAM_URL] ?: ""
        )
    }

    override val enabledApps: Flow<Set<String>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_ENABLED_APPS] ?: ""
        if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    override val keepOnline24_7: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_KEEP_ONLINE_24_7] ?: false
    }

    override val presenceEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PRESENCE_ENABLED] ?: false
    }

    override val analyticsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ANALYTICS_ENABLED] ?: true
    }

    override val userToken: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_USER_TOKEN] ?: ""
    }

    override val customRpcPresets: Flow<List<CustomRpcPreset>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_CUSTOM_RPC_PRESETS] ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<CustomRpcPreset>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(CustomRpcPreset(
                id = obj.getString("id"),
                name = obj.getString("name"),
                details = obj.optString("details", ""),
                state = obj.optString("state", ""),
                largeImage = obj.optString("largeImage", ""),
                largeText = obj.optString("largeText", ""),
                smallImage = obj.optString("smallImage", ""),
                smallText = obj.optString("smallText", ""),
                button1Label = obj.optString("button1Label", ""),
                button1Url = obj.optString("button1Url", ""),
                button2Label = obj.optString("button2Label", ""),
                button2Url = obj.optString("button2Url", ""),
                startTimestamp = obj.optLong("startTimestamp").takeIf { it > 0 },
                endTimestamp = obj.optLong("endTimestamp").takeIf { it > 0 },
                activityType = obj.optInt("activityType", 0),
                status = obj.optString("status", "online"),
                isFavorite = obj.optBoolean("isFavorite", false),
                createdAt = obj.optLong("createdAt", 0),
                updatedAt = obj.optLong("updatedAt", 0)
            ))
        }
        list
    }

    override suspend fun setUserToken(token: String) {
        dataStore.edit { prefs -> prefs[KEY_USER_TOKEN] = token }
    }

    override suspend fun setAnalyticsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_ANALYTICS_ENABLED] = enabled }
    }

    override suspend fun setKeepOnline24_7(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_KEEP_ONLINE_24_7] = enabled }
    }

    override suspend fun setPresenceEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_PRESENCE_ENABLED] = enabled }
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

    override suspend fun savePresenceConfig(presence: PresenceData) {
        dataStore.edit { prefs ->
            prefs[KEY_PRESENCE_NAME] = presence.name
            prefs[KEY_PRESENCE_DETAILS] = presence.details
            prefs[KEY_PRESENCE_STATE] = presence.state
            prefs[KEY_PRESENCE_TYPE] = presence.type
            prefs[KEY_PRESENCE_LARGE_IMAGE] = presence.largeImage
            prefs[KEY_PRESENCE_LARGE_TEXT] = presence.largeText
            prefs[KEY_PRESENCE_SMALL_IMAGE] = presence.smallImage
            prefs[KEY_PRESENCE_SMALL_TEXT] = presence.smallText
            prefs[KEY_PRESENCE_START_TIMESTAMP] = presence.startTimestamp?.toString().orEmpty()
            prefs[KEY_PRESENCE_END_TIMESTAMP] = presence.endTimestamp?.toString().orEmpty()
            prefs[KEY_PRESENCE_STATUS] = presence.status
            prefs[KEY_PRESENCE_BUTTON1_LABEL] = presence.button1Label
            prefs[KEY_PRESENCE_BUTTON1_URL] = presence.button1Url
            prefs[KEY_PRESENCE_BUTTON2_LABEL] = presence.button2Label
            prefs[KEY_PRESENCE_BUTTON2_URL] = presence.button2Url
            prefs[KEY_PRESENCE_PARTY_SIZE] = presence.partySize?.toString().orEmpty()
            prefs[KEY_PRESENCE_PARTY_MAX] = presence.partyMax?.toString().orEmpty()
            prefs[KEY_PRESENCE_PLATFORM] = presence.platform
            prefs[KEY_PRESENCE_STREAM_URL] = presence.streamUrl
        }
    }

    override suspend fun saveCustomRpcPreset(preset: CustomRpcPreset) {
        dataStore.edit { prefs ->
            val raw = prefs[KEY_CUSTOM_RPC_PRESETS] ?: "[]"
            val arr = JSONArray(raw)
            var found = false
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("id") == preset.id) {
                    obj.put("id", preset.id)
                    obj.put("name", preset.name)
                    obj.put("details", preset.details)
                    obj.put("state", preset.state)
                    obj.put("largeImage", preset.largeImage)
                    obj.put("largeText", preset.largeText)
                    obj.put("smallImage", preset.smallImage)
                    obj.put("smallText", preset.smallText)
                    obj.put("button1Label", preset.button1Label)
                    obj.put("button1Url", preset.button1Url)
                    obj.put("button2Label", preset.button2Label)
                    obj.put("button2Url", preset.button2Url)
                    obj.put("startTimestamp", preset.startTimestamp ?: 0)
                    obj.put("endTimestamp", preset.endTimestamp ?: 0)
                    obj.put("activityType", preset.activityType)
                    obj.put("status", preset.status)
                    obj.put("isFavorite", preset.isFavorite)
                    obj.put("createdAt", preset.createdAt)
                    obj.put("updatedAt", preset.updatedAt)
                    found = true
                    break
                }
            }
            if (!found) {
                arr.put(JSONObject().apply {
                    put("id", preset.id)
                    put("name", preset.name)
                    put("details", preset.details)
                    put("state", preset.state)
                    put("largeImage", preset.largeImage)
                    put("largeText", preset.largeText)
                    put("smallImage", preset.smallImage)
                    put("smallText", preset.smallText)
                    put("button1Label", preset.button1Label)
                    put("button1Url", preset.button1Url)
                    put("button2Label", preset.button2Label)
                    put("button2Url", preset.button2Url)
                    put("startTimestamp", preset.startTimestamp ?: 0)
                    put("endTimestamp", preset.endTimestamp ?: 0)
                    put("activityType", preset.activityType)
                    put("status", preset.status)
                    put("isFavorite", preset.isFavorite)
                    put("createdAt", preset.createdAt)
                    put("updatedAt", preset.updatedAt)
                })
            }
            prefs[KEY_CUSTOM_RPC_PRESETS] = arr.toString()
        }
    }

    override suspend fun deleteCustomRpcPreset(presetId: String) {
        dataStore.edit { prefs ->
            val raw = prefs[KEY_CUSTOM_RPC_PRESETS] ?: "[]"
            val arr = JSONArray(raw)
            val keep = JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("id") != presetId) {
                    keep.put(obj)
                }
            }
            prefs[KEY_CUSTOM_RPC_PRESETS] = keep.toString()
        }
    }

    override suspend fun reorderCustomRpcPresets(presets: List<CustomRpcPreset>) {
        dataStore.edit { prefs ->
            val arr = JSONArray()
            for (preset in presets) {
                arr.put(JSONObject().apply {
                    put("id", preset.id)
                    put("name", preset.name)
                    put("details", preset.details)
                    put("state", preset.state)
                    put("largeImage", preset.largeImage)
                    put("largeText", preset.largeText)
                    put("smallImage", preset.smallImage)
                    put("smallText", preset.smallText)
                    put("button1Label", preset.button1Label)
                    put("button1Url", preset.button1Url)
                    put("button2Label", preset.button2Label)
                    put("button2Url", preset.button2Url)
                    put("startTimestamp", preset.startTimestamp ?: 0)
                    put("endTimestamp", preset.endTimestamp ?: 0)
                    put("activityType", preset.activityType)
                    put("status", preset.status)
                    put("isFavorite", preset.isFavorite)
                    put("createdAt", preset.createdAt)
                    put("updatedAt", preset.updatedAt)
                })
            }
            prefs[KEY_CUSTOM_RPC_PRESETS] = arr.toString()
        }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
