package com.kyrx.mypresence.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val isOnboardingCompleted: Flow<Boolean>
    val accessToken: Flow<String?>
    val refreshToken: Flow<String?>
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun saveUserInfo(userId: String, username: String)
    suspend fun clearAll()
}
