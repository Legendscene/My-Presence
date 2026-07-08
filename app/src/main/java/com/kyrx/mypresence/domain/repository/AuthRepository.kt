package com.kyrx.mypresence.domain.repository

import com.kyrx.mypresence.data.remote.DiscordUser
import kotlinx.coroutines.flow.StateFlow

data class PendingAuthParams(
    val codeVerifier: String,
    val state: String
)

sealed class AuthState {
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(val user: DiscordUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

interface AuthRepository {
    val authState: StateFlow<AuthState>

    fun buildAuthorizationUrl(state: String, codeChallenge: String): String
    fun prepareAuthorization(): PendingAuthParams
    fun consumePendingParams(): PendingAuthParams?
    suspend fun handleAuthorizationResponse(code: String, expectedState: String, codeVerifier: String): Result<DiscordUser>
    suspend fun loadCurrentUser(): DiscordUser?
    suspend fun refreshAccessToken(): Boolean
    suspend fun logout()
    fun getAccessToken(): String?
    fun isTokenExpired(): Boolean
    fun isOAuthInProgress(): Boolean
    fun cancelOAuth()
}
