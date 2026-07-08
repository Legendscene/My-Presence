package com.kyrx.mypresence.core.gateway

sealed class GatewayConnectionState {
    data object Disconnected : GatewayConnectionState()
    data object Connecting : GatewayConnectionState()
    data class Connected(val sessionId: String, val sequence: Int?) : GatewayConnectionState()
    data class Error(val message: String) : GatewayConnectionState()
}
