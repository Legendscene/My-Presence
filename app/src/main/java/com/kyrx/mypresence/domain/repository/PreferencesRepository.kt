package com.kyrx.mypresence.domain.repository

import com.kyrx.mypresence.domain.model.AppPresenceConfig
import com.kyrx.mypresence.domain.model.CustomRpcPreset
import com.kyrx.mypresence.domain.model.PresenceData
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val isOnboardingCompleted: Flow<Boolean>
    val notificationsEnabled: Flow<Boolean>
    val autoStartEnabled: Flow<Boolean>
    val isDarkMode: Flow<Boolean>
    val presenceName: Flow<String>
    val presenceDetails: Flow<String>
    val presenceState: Flow<String>
    val presenceType: Flow<Int>
    val customPresence: Flow<PresenceData>
    val enabledApps: Flow<Set<String>>
    val keepOnline24_7: Flow<Boolean>
    val presenceEnabled: Flow<Boolean>
    val appPresenceConfigs: Flow<List<AppPresenceConfig>>
    val customRpcPresets: Flow<List<CustomRpcPreset>>
    val analyticsEnabled: Flow<Boolean>
    val userToken: Flow<String>
    suspend fun setAnalyticsEnabled(enabled: Boolean)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setUserToken(token: String)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setAutoStartEnabled(enabled: Boolean)
    suspend fun setIsDarkMode(enabled: Boolean)
    suspend fun setKeepOnline24_7(enabled: Boolean)
    suspend fun setPresenceEnabled(enabled: Boolean)
    suspend fun savePresenceConfig(name: String, details: String, state: String, type: Int)
    suspend fun savePresenceConfig(presence: PresenceData)
    suspend fun setEnabledApps(packages: Set<String>)
    suspend fun saveAppPresenceConfig(config: AppPresenceConfig)
    suspend fun removeAppPresenceConfig(packageName: String)
    suspend fun saveCustomRpcPreset(preset: CustomRpcPreset)
    suspend fun deleteCustomRpcPreset(presetId: String)
    suspend fun reorderCustomRpcPresets(presets: List<CustomRpcPreset>)
    suspend fun clearAll()
}
