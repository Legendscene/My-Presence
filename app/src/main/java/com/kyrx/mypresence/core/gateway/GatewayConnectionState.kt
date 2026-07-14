package com.kyrx.mypresence.core.gateway

import androidx.compose.runtime.Stable

@Stable
sealed interface GatewayConnectionState {
    @Stable data object Disconnected : GatewayConnectionState
    @Stable data object Connecting : GatewayConnectionState
    @Stable data object Authenticating : GatewayConnectionState
    @Stable data class Connected(
        val sessionId: String,
        val sequence: Int?
    ) : GatewayConnectionState
    @Stable data object Resuming : GatewayConnectionState
    @Stable data object Reconnecting : GatewayConnectionState
    @Stable data object HeartbeatLost : GatewayConnectionState
    @Stable data class RateLimited(val retryAfterMs: Long) : GatewayConnectionState
    @Stable data class InvalidSession(val canResume: Boolean) : GatewayConnectionState
    @Stable data class Unauthorized(val message: String) : GatewayConnectionState
    @Stable data class FatalError(val message: String) : GatewayConnectionState

    val label: String get() = when (this) {
        is Disconnected -> "Disconnected"
        is Connecting -> "Connecting"
        is Authenticating -> "Authenticating"
        is Connected -> "Connected"
        is Resuming -> "Resuming"
        is Reconnecting -> "Reconnecting"
        is HeartbeatLost -> "Heartbeat Lost"
        is RateLimited -> "Rate Limited"
        is InvalidSession -> "Invalid Session"
        is Unauthorized -> "Unauthorized"
        is FatalError -> "Fatal Error"
    }
}
