package com.kyrx.mypresence.domain.repository

import com.kyrx.mypresence.core.gateway.GatewayConnectionState
import com.kyrx.mypresence.core.gateway.GatewayDiagnostics
import com.kyrx.mypresence.core.gateway.GatewayEvent
import com.kyrx.mypresence.domain.model.PresenceData
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface GatewayRepository {
    val state: StateFlow<GatewayConnectionState>
    val diagnostics: StateFlow<GatewayDiagnostics>
    val events: SharedFlow<GatewayEvent>

    suspend fun connect()
    fun disconnect()
    fun setMaxReconnectAttempts(max: Int)
    suspend fun updatePresence(presence: PresenceData)
    suspend fun clearPresence()
    fun destroy()
}
