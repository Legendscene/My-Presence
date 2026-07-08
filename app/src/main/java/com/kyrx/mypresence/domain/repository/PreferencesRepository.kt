package com.kyrx.mypresence.domain.repository

import com.kyrx.mypresence.domain.model.AppPresenceConfig
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
    val enabledApps: Flow<Set<String>>
    val keepOnline24_7: Flow<Boolean>
    val appPresenceConfigs: Flow<List<AppPresenceConfig>>
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setAutoStartEnabled(enabled: Boolean)
    suspend fun setIsDarkMode(enabled: Boolean)
    suspend fun setKeepOnline24_7(enabled: Boolean)
    suspend fun savePresenceConfig(name: String, details: String, state: String, type: Int)
    suspend fun setEnabledApps(packages: Set<String>)
    suspend fun saveAppPresenceConfig(config: AppPresenceConfig)
    suspend fun removeAppPresenceConfig(packageName: String)
    suspend fun clearAll()
}
