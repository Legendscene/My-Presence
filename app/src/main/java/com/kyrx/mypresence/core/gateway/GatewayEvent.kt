package com.kyrx.mypresence.core.gateway

sealed interface GatewayEvent {
    data object Connected : GatewayEvent
    data object Disconnected : GatewayEvent
    data class HeartbeatSent(val sequence: Int?) : GatewayEvent
    data class HeartbeatAcknowledged(val latencyMs: Long) : GatewayEvent
    data class HeartbeatLost(val missedCount: Int) : GatewayEvent
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : GatewayEvent
    data object ResumeSucceeded : GatewayEvent
    data object ResumeFailed : GatewayEvent
    data class Dispatch(val type: String) : GatewayEvent
    data class Error(val message: String) : GatewayEvent
    data class RateLimited(val retryAfterMs: Long) : GatewayEvent
}
