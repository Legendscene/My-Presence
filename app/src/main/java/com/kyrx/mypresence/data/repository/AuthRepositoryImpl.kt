package com.kyrx.mypresence.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import java.security.MessageDigest
import androidx.security.crypto.MasterKey
import com.kyrx.mypresence.core.analytics.CrashReporter
import com.kyrx.mypresence.core.auth.TokenIntegrity
import com.kyrx.mypresence.core.utils.Constants
import com.kyrx.mypresence.core.utils.PKCE
import com.kyrx.mypresence.data.remote.DiscordApi
import com.kyrx.mypresence.domain.repository.PendingAuthParams
import com.kyrx.mypresence.data.remote.DiscordUser
import com.kyrx.mypresence.domain.repository.AuthRepository
import com.kyrx.mypresence.domain.repository.AuthState
import com.kyrx.mypresence.domain.repository.GoogleAccountInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val discordApi: DiscordApi,
    private val crashReporter: CrashReporter,
    private val preferencesRepository: PreferencesRepository
) : AuthRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        try {
            EncryptedSharedPreferences.create(
                context,
                "discord_auth_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: java.security.GeneralSecurityException) {
            crashReporter.log("SecurePrefs corruption: ${e::class.simpleName} - ${e.message}")
            context.getSharedPreferences("discord_auth_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            EncryptedSharedPreferences.create(
                context,
                "discord_auth_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
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
        const val KEY_BANNER_COLOR = "banner_color"
        const val KEY_BIO = "bio"
        const val KEY_PREMIUM_TYPE = "premium_type"
        const val KEY_PUBLIC_FLAGS = "public_flags"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_PRONOUNS = "pronouns"
        const val KEY_NAMEPLATE = "nameplate"
        const val KEY_ACCENT_COLOR = "accent_color"
        const val KEY_LOCALE = "locale"
        const val KEY_EMAIL = "email"
        const val KEY_VERIFIED = "verified"
        const val KEY_MFA_ENABLED = "mfa_enabled"
        const val KEY_PHONE = "phone"
        const val KEY_USER_FLAGS = "user_flags"

        const val KEY_GOOGLE_ID_TOKEN = "google_id_token"
        const val KEY_GOOGLE_DISPLAY_NAME = "google_display_name"
        const val KEY_GOOGLE_EMAIL = "google_email"

        private const val EXPIRY_BUFFER_MS = 300_000L
    }

    init {
        restoreSession()
    }

    private fun restoreSession() {
        scope.launch {
            val cachedUser = loadCachedUser()

            val oauthToken = getAccessToken()
            val userToken = preferencesRepository.userToken.first()

            if (oauthToken != null && !isTokenExpired()) {
                Log.d("AuthRepo", "Found valid OAuth token, restoring session")
                if (cachedUser != null) {
                    _authState.value = AuthState.Authenticated(cachedUser)
                    crashReporter.setUserId(cachedUser.id)
                }
                scope.launch {
                    try {
                        val freshUser = discordApi.getCurrentUser(oauthToken)
                        cacheUser(freshUser)
                        crashReporter.setUserId(freshUser.id)
                        _authState.value = AuthState.Authenticated(freshUser)
                    } catch (e: Exception) {
                        Log.e("AuthRepo", "Background user fetch failed: ${e.message}")
                    }
                }
                return@launch
            }

            if (oauthToken != null && isTokenExpired()) {
                Log.d("AuthRepo", "OAuth token expired, attempting refresh...")
                try {
                    val rt = securePrefs.getString(KEY_REFRESH_TOKEN, null)
                    if (rt != null) {
                        val tokenResponse = discordApi.refreshToken(rt, Constants.CLIENT_ID)
                        saveTokens(tokenResponse)
                        preferencesRepository.setUserToken(tokenResponse.access_token)
                        if (cachedUser != null) {
                            _authState.value = AuthState.Authenticated(cachedUser)
                            crashReporter.setUserId(cachedUser.id)
                        }
                        scope.launch {
                            try {
                                val freshUser = discordApi.getCurrentUser(tokenResponse.access_token)
                                cacheUser(freshUser)
                                crashReporter.setUserId(freshUser.id)
                                _authState.value = AuthState.Authenticated(freshUser)
                            } catch (e: Exception) {
                                Log.e("AuthRepo", "Background user fetch after refresh failed: ${e.message}")
                            }
                        }
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.e("AuthRepo", "Token refresh failed: ${e.message}")
                }
            }

            if (userToken.isNotBlank()) {
                Log.d("AuthRepo", "Found user token, restoring session")
                if (cachedUser != null) {
                    _authState.value = AuthState.Authenticated(cachedUser)
                    crashReporter.setUserId(cachedUser.id)
                }
                scope.launch {
                    try {
                        val freshUser = discordApi.getCurrentUserWithUserToken(userToken)
                        cacheUser(freshUser)
                        crashReporter.setUserId(freshUser.id)
                        _authState.value = AuthState.Authenticated(freshUser)
                    } catch (e: Exception) {
                        Log.e("AuthRepo", "Background user fetch with user token failed: ${e.message}")
                    }
                }
                if (cachedUser != null) return@launch
            }

            _authState.value = AuthState.Unauthenticated
        }
    }

    override suspend fun loginWithToken(userToken: String): DiscordUser {
        val user = discordApi.getCurrentUserWithUserToken(userToken)
        val storedHash = userToken.tokenHash()
        Log.i("TOKEN_LIFECYCLE", "  STORED    hash=${storedHash}  at AuthRepositoryImpl.loginWithToken (DataStore save)")
        TokenIntegrity.verifyToken("AuthRepo.beforeSave", userToken, "Before DataStore save")
        preferencesRepository.setUserToken(userToken)
        val reloadedToken = preferencesRepository.userToken.first()
        TokenIntegrity.verifyToken("AuthRepo.afterLoad", reloadedToken, "After DataStore re-read")
        if (reloadedToken != userToken) {
            Log.e("TOKEN_INTEGRITY", "⚠ DATASTORE CORRUPTION: token changed after save/load cycle!")
            Log.e("TOKEN_INTEGRITY", "  Original length: ${userToken.length}")
            Log.e("TOKEN_INTEGRITY", "  Reloaded length: ${reloadedToken.length}")
        } else {
            Log.d("TOKEN_INTEGRITY", "✓ DataStore save/load cycle preserved token exactly")
        }
        cacheUser(user)
        crashReporter.setUserId(user.id)
        _authState.value = AuthState.Authenticated(user)
        return user
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
            crashReporter.setUserId(user.id)

            preferencesRepository.setUserToken(tokenResponse.access_token)

            _authState.value = AuthState.Authenticated(user)
            Result.success(user)
        } catch (e: Exception) {
            crashReporter.logNonFatal(e)
            _authState.value = AuthState.Error(e.message ?: "Authorization failed")
            Result.failure(e)
        }
    }

    override suspend fun loadCurrentUser(): DiscordUser? {
        val userToken = preferencesRepository.userToken.first()
        if (userToken.isNotBlank()) {
            return try {
                val user = discordApi.getCurrentUserWithUserToken(userToken)
                cacheUser(user)
                _authState.value = AuthState.Authenticated(user)
                user
            } catch (e: Exception) {
                val cached = loadCachedUser()
                if (cached != null) {
                    _authState.value = AuthState.Authenticated(cached)
                    cached
                } else null
            }
        }

        val token = getAccessToken() ?: return null
        return try {
            val user = discordApi.getCurrentUser(token)
            cacheUser(user)
            _authState.value = AuthState.Authenticated(user)
            user
        } catch (e: Exception) {
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
        preferencesRepository.setUserToken("")
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

    override fun getUserToken(): String? {
        return runBlocking {
            try {
                preferencesRepository.userToken.first().takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                null
            }
        }
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

    override fun getUserIdForCrashReporting(): String? {
        return try {
            securePrefs.getString(KEY_USER_ID, null)
        } catch (e: Exception) {
            crashReporter.log("getUserIdForCrashReporting failed: ${e.message}")
            null
        }
    }

    override fun getGoogleAccount(): GoogleAccountInfo? {
        val idToken = securePrefs.getString(KEY_GOOGLE_ID_TOKEN, null) ?: return null
        return GoogleAccountInfo(
            idToken = idToken,
            displayName = securePrefs.getString(KEY_GOOGLE_DISPLAY_NAME, null),
            email = securePrefs.getString(KEY_GOOGLE_EMAIL, null)
        )
    }

    override suspend fun saveGoogleAccount(idToken: String, displayName: String?, email: String?) {
        securePrefs.edit()
            .putString(KEY_GOOGLE_ID_TOKEN, idToken)
            .putString(KEY_GOOGLE_DISPLAY_NAME, displayName)
            .putString(KEY_GOOGLE_EMAIL, email)
            .apply()
        Log.i("AuthRepo", "Google account saved: displayName=$displayName email=$email")
    }

    private fun cacheUser(user: DiscordUser) {
        securePrefs.edit()
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_DISCRIMINATOR, user.discriminator)
            .putString(KEY_AVATAR, user.avatar)
            .putString(KEY_GLOBAL_NAME, user.global_name)
            .putString(KEY_DISPLAY_NAME, user.display_name)
            .putString(KEY_BANNER, user.banner)
            .putString(KEY_BANNER_COLOR, user.banner_color)
            .putString(KEY_AVATAR_DECORATION, user.avatar_decoration_data?.asset)
            .putString(KEY_BIO, user.bio)
            .putString(KEY_PRONOUNS, user.pronouns)
            .putString(KEY_LOCALE, user.locale)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_PHONE, user.phone)
            .putInt(KEY_PREMIUM_TYPE, user.premium_type ?: 0)
            .putInt(KEY_PUBLIC_FLAGS, user.public_flags ?: 0)
            .putInt(KEY_NAMEPLATE, user.nameplate ?: 0)
            .putInt(KEY_ACCENT_COLOR, user.accent_color ?: 0)
            .putInt(KEY_USER_FLAGS, user.flags ?: 0)
            .putBoolean(KEY_VERIFIED, user.verified ?: false)
            .putBoolean(KEY_MFA_ENABLED, user.mfa_enabled ?: false)
            .apply()
    }

    private fun loadCachedUser(): DiscordUser? {
        val id = securePrefs.getString(KEY_USER_ID, null) ?: return null
        val decoAsset = securePrefs.getString(KEY_AVATAR_DECORATION, null)
        val premiumType = securePrefs.getInt(KEY_PREMIUM_TYPE, 0)
        val publicFlags = securePrefs.getInt(KEY_PUBLIC_FLAGS, 0)
        val nameplateVal = securePrefs.getInt(KEY_NAMEPLATE, 0)
        val accentColorVal = securePrefs.getInt(KEY_ACCENT_COLOR, 0)
        val userFlagsVal = securePrefs.getInt(KEY_USER_FLAGS, 0)
        return DiscordUser(
            id = id,
            username = securePrefs.getString(KEY_USERNAME, "") ?: "",
            discriminator = securePrefs.getString(KEY_DISCRIMINATOR, "0") ?: "0",
            global_name = securePrefs.getString(KEY_GLOBAL_NAME, null),
            display_name = securePrefs.getString(KEY_DISPLAY_NAME, null),
            avatar = securePrefs.getString(KEY_AVATAR, null),
            banner = securePrefs.getString(KEY_BANNER, null),
            banner_color = securePrefs.getString(KEY_BANNER_COLOR, null),
            accent_color = if (accentColorVal != 0) accentColorVal else null,
            avatar_decoration_data = if (decoAsset != null) com.kyrx.mypresence.data.remote.AvatarDecorationData(asset = decoAsset) else null,
            bio = securePrefs.getString(KEY_BIO, null),
            pronouns = securePrefs.getString(KEY_PRONOUNS, null),
            nameplate = if (nameplateVal != 0) nameplateVal else null,
            locale = securePrefs.getString(KEY_LOCALE, null),
            email = securePrefs.getString(KEY_EMAIL, null),
            phone = securePrefs.getString(KEY_PHONE, null),
            verified = securePrefs.getBoolean(KEY_VERIFIED, false),
            mfa_enabled = securePrefs.getBoolean(KEY_MFA_ENABLED, false),
            flags = if (userFlagsVal != 0) userFlagsVal else null,
            premium_type = if (premiumType > 0) premiumType else null,
            public_flags = if (publicFlags != 0) publicFlags else null
        )
    }

    private fun clearAll() {
        securePrefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRY).remove(KEY_USER_ID).remove(KEY_USERNAME)
            .remove(KEY_DISCRIMINATOR).remove(KEY_AVATAR).remove(KEY_GLOBAL_NAME)
            .remove(KEY_DISPLAY_NAME).remove(KEY_BANNER).remove(KEY_AVATAR_DECORATION)
            .remove(KEY_BANNER_COLOR).remove(KEY_BIO).remove(KEY_PREMIUM_TYPE)
            .remove(KEY_PUBLIC_FLAGS).remove(KEY_PRONOUNS).remove(KEY_NAMEPLATE)
            .remove(KEY_ACCENT_COLOR).remove(KEY_LOCALE).remove(KEY_EMAIL)
            .remove(KEY_VERIFIED).remove(KEY_MFA_ENABLED).remove(KEY_PHONE)
            .remove(KEY_USER_FLAGS)
            .apply()
    }

    private fun String.tokenHash(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(this.toByteArray(Charsets.UTF_8))
        return digest.take(8).joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }
}
