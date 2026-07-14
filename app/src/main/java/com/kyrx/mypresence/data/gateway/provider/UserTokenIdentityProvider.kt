package com.kyrx.mypresence.data.gateway.provider

import android.content.Context
import android.util.Log
import com.kyrx.mypresence.core.gateway.GatewayIdentityProvider
import com.kyrx.mypresence.core.gateway.IdentifyPayload
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserTokenIdentityProvider @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context
) : GatewayIdentityProvider {

    override val name: String = "UserToken"

    override fun hasToken(): Boolean = runBlocking {
        preferencesRepository.userToken.first().isNotBlank()
    }

    override suspend fun provideIdentity(): IdentifyPayload? {
        val rawToken = preferencesRepository.userToken.first()
        val token = rawToken.trim()
        if (token.isBlank()) return null
        com.kyrx.mypresence.core.auth.TokenIntegrity.verifyToken(
            "UserTokenProvider.provideIdentity",
            token,
            "Gateway IDENTIFY (ready to send)"
        )
        val hasWhitespace = token.any { c: Char -> c.isWhitespace() }
        val containsPrefix = token.contains("Bot", ignoreCase = true) || token.contains("Bearer", ignoreCase = true)
        val charCategories = token.take(30).map { c: Char ->
            when {
                c.isLetterOrDigit() -> c
                c.isWhitespace() -> "[WS]"
                c == '.' -> "."
                c == '_' -> "_"
                c == '-' -> "-"
                else -> "[0x${c.toInt().toString(16)}]"
            }
        }.joinToString("")
        Log.d("GATEWAY_PIPELINE", "[AUTH] UserTokenProvider: token length=${token.length} hasWhitespace=$hasWhitespace first8=\"${token.take(8)}\" hasBotOrBearerPrefix=$containsPrefix")
        Log.d("GATEWAY_PIPELINE", "[AUTH] Char categories (first 30): $charCategories")
        Log.d("GATEWAY_PIPELINE", "[AUTH] Token ready for IDENTIFY: length=${token.length} UTF-8 bytes=${token.toByteArray(StandardCharsets.UTF_8).size}")
        Log.d("GATEWAY_PIPELINE", "[AUTH] Raw char codes first 20: ${token.take(20).map { c: Char -> c.toInt() }}")
        if (containsPrefix) {
            Log.w("GATEWAY_PIPELINE", "[AUTH] WARNING: Token contains 'Bot' or 'Bearer' prefix! Discord may reject this.")
        }

        return IdentifyPayload(
            token = token,
            capabilities = 65,
            compress = false,
            largeThreshold = 100,
            properties = mapOf(
                "os" to "Windows",
                "browser" to "Discord Client",
                "device" to "ktor"
            )
        )
    }

    override suspend fun onSessionInvalidated() {
    }

    override fun onAuthError(error: String) {
    }
}
