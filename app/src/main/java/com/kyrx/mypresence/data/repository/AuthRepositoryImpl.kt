package com.kyrx.mypresence.data.repository

import com.kyrx.mypresence.data.remote.DiscordApi
import com.kyrx.mypresence.data.remote.DiscordUser
import com.kyrx.mypresence.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val discordApi: DiscordApi
) : AuthRepository {

    private val _currentUser = MutableStateFlow<DiscordUser?>(null)
    override val currentUser: StateFlow<DiscordUser?> = _currentUser.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    override suspend fun exchangeCode(code: String): Result<String> {
        return try {
            val tokenResponse = discordApi.exchangeCode(
                code = code,
                clientId = CLIENT_ID,
                clientSecret = CLIENT_SECRET,
                redirectUri = REDIRECT_URI
            )
            _isAuthenticated.value = true
            Result.success(tokenResponse.access_token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loadCurrentUser(accessToken: String): Result<DiscordUser> {
        return try {
            val user = discordApi.getCurrentUser(accessToken)
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        _currentUser.value = null
        _isAuthenticated.value = false
    }

    companion object {
        private const val CLIENT_ID = ""
        private const val CLIENT_SECRET = ""
        private const val REDIRECT_URI = "mypresence://oauth2/callback"
    }
}
