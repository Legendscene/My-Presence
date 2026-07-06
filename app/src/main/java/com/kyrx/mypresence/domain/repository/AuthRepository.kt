package com.kyrx.mypresence.domain.repository

import com.kyrx.mypresence.data.remote.DiscordUser
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<DiscordUser?>
    val isAuthenticated: StateFlow<Boolean>
    suspend fun exchangeCode(code: String): Result<String>
    suspend fun loadCurrentUser(accessToken: String): Result<DiscordUser>
    suspend fun authenticateWithToken(token: String): Result<DiscordUser>
    suspend fun logout()
}
