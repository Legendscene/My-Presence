package com.kyrx.mypresence.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.kyrx.mypresence.core.utils.Constants
import com.kyrx.mypresence.core.utils.PKCE
import com.kyrx.mypresence.data.remote.DiscordApi
import com.kyrx.mypresence.domain.repository.PendingAuthParams
import com.kyrx.mypresence.data.remote.DiscordUser
import com.kyrx.mypresence.domain.repository.AuthRepository
import com.kyrx.mypresence.domain.repository.AuthState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val discordApi: DiscordApi
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "discord_auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // PKCE params stored in memory only — lost if process dies (security)
    @Volatile
    private var pendingParams: PendingAuthParams? = null

    override fun consumePendingParams(): PendingAuthParams? {
        val params = pendingParams
        pendingParams = null
        return params
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_TOKEN_EXPIRY = "token_expiry"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_DISCRIMINATOR = "discriminator"
        const val KEY_AVATAR = "avatar"
        const val KEY_GLOBAL_NAME = "global_name"
        const val KEY_BANNER = "banner"
        const val KEY_AVATAR_DECORATION = "avatar_decoration"

        // 5-minute buffer before actual expiry
        private const val EXPIRY_BUFFER_MS = 300_000L
    }

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val token = getAccessToken()
        if (token != null && !isTokenExpired()) {
            val user = loadCachedUser()
            if (user != null) {
                _authState.value = AuthState.Authenticated(user)
                return
            }
        }
        if (token != null && isTokenExpired()) {
            // Token expired — will need refresh
            _authState.value = AuthState.Unauthenticated
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    override fun isOAuthInProgress(): Boolean = pendingParams != null

    override fun cancelOAuth() {
        pendingParams = null
        _authState.value = AuthState.Error("Authorization cancelled")
    }

    override fun prepareAuthorization(): PendingAuthParams {
        val codeVerifier = PKCE.generateCodeVerifier()
        val state = PKCE.generateState()
        val params = PendingAuthParams(codeVerifier, state)
        pendingParams = params
        return params
    }

    override fun buildAuthorizationUrl(state: String, codeChallenge: String): String {
        val params = mapOf(
            "response_type" to "code",
            "client_id" to Constants.CLIENT_ID,
            "redirect_uri" to Constants.REDIRECT_URI,
            "scope" to Constants.SCOPE,
            "state" to state,
            "code_challenge_method" to "S256",
            "code_challenge" to codeChallenge
        )
        return Constants.DISCORD_OAUTH2_URL + "?" + params.entries.joinToString("&") {
            "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}"
        }
    }

    override suspend fun handleAuthorizationResponse(
        code: String,
        expectedState: String,
        codeVerifier: String
    ): Result<DiscordUser> {
        return try {
            val tokenResponse = discordApi.exchangeCode(
                code = code,
                codeVerifier = codeVerifier,
                clientId = Constants.CLIENT_ID,
                redirectUri = Constants.REDIRECT_URI
            )

            saveTokens(tokenResponse)

            val user = discordApi.getCurrentUser(tokenResponse.access_token)
            cacheUser(user)

            _authState.value = AuthState.Authenticated(user)
            Result.success(user)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Authorization failed")
            Result.failure(e)
        }
    }

    override suspend fun loadCurrentUser(): DiscordUser? {
        val token = getAccessToken() ?: return null
        return try {
            val user = discordApi.getCurrentUser(token)
            cacheUser(user)
            _authState.value = AuthState.Authenticated(user)
            user
        } catch (e: Exception) {
            // Try refresh in case token expired
            var refreshedUser: DiscordUser? = null
            if (isTokenExpired()) {
                refreshAccessToken()
                val newToken = getAccessToken()
                if (newToken != null) {
                    try {
                        val user = discordApi.getCurrentUser(newToken)
                        cacheUser(user)
                        _authState.value = AuthState.Authenticated(user)
                        refreshedUser = user
                    } catch (_: Exception) {}
                }
            }
            if (refreshedUser != null) {
                refreshedUser
            } else {
                val cached = loadCachedUser()
                if (cached != null) {
                    _authState.value = AuthState.Authenticated(cached)
                    cached
                } else null
            }
        }
    }

    override suspend fun refreshAccessToken(): Boolean {
        val refreshToken = securePrefs.getString(KEY_REFRESH_TOKEN, null) ?: return false
        return try {
            val tokenResponse = discordApi.refreshToken(
                refreshToken = refreshToken,
                clientId = Constants.CLIENT_ID
            )
            saveTokens(tokenResponse)
            true
        } catch (e: Exception) {
            clearAll()
            _authState.value = AuthState.Unauthenticated
            false
        }
    }

    override suspend fun logout() {
        val token = getAccessToken()
        if (token != null) {
            try {
                discordApi.revokeToken(token, Constants.CLIENT_ID)
            } catch (_: Exception) {}
        }
        clearAll()
        _authState.value = AuthState.Unauthenticated
    }

    override fun getAccessToken(): String? {
        val expiry = securePrefs.getLong(KEY_TOKEN_EXPIRY, 0L)
        if (expiry == 0L) return null
        return securePrefs.getString(KEY_ACCESS_TOKEN, null)
    }

    override fun isTokenExpired(): Boolean {
        val expiry = securePrefs.getLong(KEY_TOKEN_EXPIRY, 0L)
        if (expiry == 0L) return true
        return System.currentTimeMillis() + EXPIRY_BUFFER_MS >= expiry
    }

    private fun saveTokens(response: com.kyrx.mypresence.data.remote.TokenResponse) {
        val expiry = System.currentTimeMillis() + (response.expires_in * 1000L)
        securePrefs.edit()
            .putString(KEY_ACCESS_TOKEN, response.access_token)
            .putString(KEY_REFRESH_TOKEN, response.refresh_token)
            .putLong(KEY_TOKEN_EXPIRY, expiry)
            .apply()
    }

    private fun cacheUser(user: DiscordUser) {
        securePrefs.edit()
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_DISCRIMINATOR, user.discriminator)
            .putString(KEY_AVATAR, user.avatar)
            .putString(KEY_GLOBAL_NAME, user.global_name)
            .putString(KEY_BANNER, user.banner)
            .putString(KEY_AVATAR_DECORATION, user.avatar_decoration_data?.asset)
            .apply()
    }

    private fun loadCachedUser(): DiscordUser? {
        val id = securePrefs.getString(KEY_USER_ID, null) ?: return null
        val decoAsset = securePrefs.getString(KEY_AVATAR_DECORATION, null)
        return DiscordUser(
            id = id,
            username = securePrefs.getString(KEY_USERNAME, "") ?: "",
            discriminator = securePrefs.getString(KEY_DISCRIMINATOR, "0") ?: "0",
            global_name = securePrefs.getString(KEY_GLOBAL_NAME, null),
            avatar = securePrefs.getString(KEY_AVATAR, null),
            banner = securePrefs.getString(KEY_BANNER, null),
            avatar_decoration_data = if (decoAsset != null) com.kyrx.mypresence.data.remote.AvatarDecorationData(asset = decoAsset) else null
        )
    }

    private fun clearAll() {
        securePrefs.edit().clear().apply()
    }
}
