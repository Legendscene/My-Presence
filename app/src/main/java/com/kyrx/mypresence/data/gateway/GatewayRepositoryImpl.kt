package com.kyrx.mypresence.data.gateway

import android.util.Log
import com.kyrx.mypresence.core.gateway.GatewayConnectionState
import com.kyrx.mypresence.core.gateway.GatewayDiagnostics
import com.kyrx.mypresence.core.gateway.GatewayEngine
import com.kyrx.mypresence.core.gateway.GatewayEvent
import com.kyrx.mypresence.core.gateway.GatewayIdentityProvider
import com.kyrx.mypresence.data.gateway.provider.BotIdentityProvider
import com.kyrx.mypresence.data.gateway.provider.UserTokenIdentityProvider
import com.kyrx.mypresence.domain.model.PresenceData
import com.kyrx.mypresence.domain.repository.GatewayRepository
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GatewayRepositoryImpl @Inject constructor(
    private val engine: GatewayEngine,
    private val botProvider: BotIdentityProvider,
    private val userTokenProvider: UserTokenIdentityProvider
) : GatewayRepository {

    override val state: StateFlow<GatewayConnectionState> = engine.state
    override val diagnostics: StateFlow<GatewayDiagnostics> = engine.diagnostics
    override val events: SharedFlow<GatewayEvent> = engine.events

    override suspend fun connect() {
        val userHasToken = userTokenProvider.hasToken()
        val provider: GatewayIdentityProvider = if (userHasToken) {
            Log.d("GATEWAY_PIPELINE", "[AUTH] Using UserTokenIdentityProvider (hasToken=true)")
            userTokenProvider
        } else {
            Log.d("GATEWAY_PIPELINE", "[AUTH] UserToken hasToken=false, falling back to BotIdentityProvider")
            botProvider
        }
        engine.connect(provider)
    }

    override fun disconnect() {
        engine.disconnect()
    }

    override fun setMaxReconnectAttempts(max: Int) {
        engine.setMaxReconnectAttempts(max)
    }

    override suspend fun updatePresence(presence: PresenceData) {
        Log.i("PRESENCE_TRACE", "REPO_UPDATE: name=${presence.name} largeImage=${presence.largeImage} thread=${Thread.currentThread().id}")
        engine.updatePresence(presence)
    }

    override suspend fun clearPresence() {
        engine.clearPresence()
    }

    override fun destroy() {
        engine.destroy()
    }
}
