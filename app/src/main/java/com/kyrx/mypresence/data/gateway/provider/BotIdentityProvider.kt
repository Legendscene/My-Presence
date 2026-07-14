package com.kyrx.mypresence.data.gateway.provider

import com.kyrx.mypresence.BuildConfig
import com.kyrx.mypresence.core.gateway.GatewayIdentityProvider
import com.kyrx.mypresence.core.gateway.IdentifyPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BotIdentityProvider @Inject constructor() : GatewayIdentityProvider {

    override val name: String = "Bot"

    override fun hasToken(): Boolean = BuildConfig.DISCORD_BOT_TOKEN.isNotBlank()

    override suspend fun provideIdentity(): IdentifyPayload? {
        val token = BuildConfig.DISCORD_BOT_TOKEN
        if (token.isBlank()) return null
        return IdentifyPayload(
            token = token,
            intents = 1,
            properties = mapOf(
                "os" to "Android",
                "browser" to "My Presence",
                "device" to "Android"
            )
        )
    }

    override suspend fun onSessionInvalidated() {
    }

    override fun onAuthError(error: String) {
    }
}
