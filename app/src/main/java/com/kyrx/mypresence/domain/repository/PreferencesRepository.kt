package com.kyrx.mypresence.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val isOnboardingCompleted: Flow<Boolean>
    val accessToken: Flow<String?>
    val refreshToken: Flow<String?>
    val notificationsEnabled: Flow<Boolean>
    val autoStartEnabled: Flow<Boolean>
    val presenceName: Flow<String>
    val presenceDetails: Flow<String>
    val presenceState: Flow<String>
    val presenceType: Flow<Int>
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun saveUserInfo(userId: String, username: String)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setAutoStartEnabled(enabled: Boolean)
    suspend fun savePresenceConfig(name: String, details: String, state: String, type: Int)
    suspend fun clearAll()
}
