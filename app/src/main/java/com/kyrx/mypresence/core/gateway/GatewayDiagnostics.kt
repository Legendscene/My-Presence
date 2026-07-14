package com.kyrx.mypresence.core.gateway

import androidx.compose.runtime.Stable

@Stable
data class GatewayDiagnostics(
    val state: GatewayConnectionState = GatewayConnectionState.Disconnected,
    val stateLabel: String = "Disconnected",
    val gatewayVersion: Int = 10,
    val heartbeatPing: Long? = null,
    val sessionId: String? = null,
    val lastSequenceNumber: Int? = null,
    val reconnectAttempts: Int = 0,
    val currentPresencePayload: String? = null,
    val currentForegroundApp: String? = null,
    val hasUsageStatsPermission: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val foregroundServiceActive: Boolean = false,
    val currentAuthProvider: String? = null,
    val lastCloseCode: Int? = null,
    val lastCloseReason: String? = null,
    val authMilestone: String? = null,
    val readyReceived: Boolean = false,
    val heartbeatsSent: Int = 0,
    val heartbeatAckCount: Int = 0,
    val lastHeartbeatAckMs: Long? = null,
    val lastOp3Timestamp: Long? = null,
    val lastActivityJson: String? = null,
    val lastGatewayDispatch: String? = null,
    val lastOp3Accepted: Boolean = false,
    val presenceUpdateCount: Int = 0,
    val eventLog: List<String> = emptyList(),
    val connectionStartedAt: Long? = null,
    val activeWebSocketCount: Int = 0,
    val isCoroutineScopeActive: Boolean = false,
    val lastHeartbeatAckElapsed: Long? = null
)
