package com.kyrx.mypresence.core.gateway

import com.kyrx.mypresence.domain.model.PresenceData
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface GatewayEngine {
    val state: StateFlow<GatewayConnectionState>
    val diagnostics: StateFlow<GatewayDiagnostics>
    val events: SharedFlow<GatewayEvent>

    fun connect(provider: GatewayIdentityProvider)
    fun disconnect()
    fun setMaxReconnectAttempts(max: Int)
    suspend fun updatePresence(presence: PresenceData)
    suspend fun clearPresence()
    fun destroy()
}
