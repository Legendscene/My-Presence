package com.kyrx.mypresence.core.gateway

data class IdentifyPayload(
    val token: String,
    val properties: Map<String, String>,
    val intents: Int? = null,
    val capabilities: Int? = null,
    val compress: Boolean? = null,
    val largeThreshold: Int? = null
)

interface GatewayIdentityProvider {
    val name: String

    suspend fun provideIdentity(): IdentifyPayload?

    fun hasToken(): Boolean

    suspend fun onSessionInvalidated()

    fun onAuthError(error: String)
}
