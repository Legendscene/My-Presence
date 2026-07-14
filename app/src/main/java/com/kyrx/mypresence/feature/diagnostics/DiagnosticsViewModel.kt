package com.kyrx.mypresence.feature.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyrx.mypresence.core.gateway.GatewayConnectionState
import com.kyrx.mypresence.core.gateway.GatewayDiagnostics
import com.kyrx.mypresence.core.gateway.GatewayEvent
import com.kyrx.mypresence.domain.repository.GatewayRepository
import com.kyrx.mypresence.domain.usecase.TogglePresenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val gatewayRepository: GatewayRepository,
    private val togglePresence: TogglePresenceUseCase
) : ViewModel() {

    val gatewayState: StateFlow<GatewayConnectionState> = gatewayRepository.state
    val diagnostics: StateFlow<GatewayDiagnostics> = gatewayRepository.diagnostics

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private var logIdCounter = 0L

    init {
        addLog("ViewModel initialized", LogEntry.Level.DEBUG)
        addLog("Gateway state: ${gatewayState.value}", LogEntry.Level.INFO)
        observeEvents()
    }

    private fun observeEvents() {
        viewModelScope.launch {
            gatewayRepository.events.collect { event ->
                val message = when (event) {
                    is GatewayEvent.Connected -> "Gateway connected"
                    is GatewayEvent.Disconnected -> "Gateway disconnected"
                    is GatewayEvent.HeartbeatSent -> "Heartbeat sent (seq=${event.sequence})"
                    is GatewayEvent.HeartbeatAcknowledged -> "Heartbeat ACK (latency=${event.latencyMs}ms)"
                    is GatewayEvent.HeartbeatLost -> "Heartbeat lost (missed=${event.missedCount})"
                    is GatewayEvent.Reconnecting -> "Reconnecting (attempt ${event.attempt}/${event.maxAttempts})"
                    is GatewayEvent.ResumeSucceeded -> "Session resumed"
                    is GatewayEvent.ResumeFailed -> "Session resume failed"
                    is GatewayEvent.Dispatch -> "Dispatch: ${event.type}"
                    is GatewayEvent.Error -> "Error: ${event.message}"
                    is GatewayEvent.RateLimited -> "Rate limited (retry in ${event.retryAfterMs}ms)"
                }
                val level = when (event) {
                    is GatewayEvent.Error, is GatewayEvent.HeartbeatLost -> LogEntry.Level.ERROR
                    is GatewayEvent.RateLimited -> LogEntry.Level.WARN
                    else -> LogEntry.Level.INFO
                }
                addLog(message, level)
            }
        }
    }

    private fun addLog(message: String, level: LogEntry.Level = LogEntry.Level.INFO) {
        _logs.value = _logs.value + LogEntry(++logIdCounter, message, level = level)
    }

    fun testGatewayConnection() {
        viewModelScope.launch {
            addLog("Testing gateway connection...", LogEntry.Level.INFO)
            togglePresence.start()
            addLog("Gateway state: ${gatewayState.value}", LogEntry.Level.INFO)
        }
    }

    fun forceReidentify() {
        viewModelScope.launch {
            addLog("Forcing re-identify via reconnect...", LogEntry.Level.WARN)
            gatewayRepository.disconnect()
            addLog("Disconnected. Reconnect manually via Presence toggle.", LogEntry.Level.INFO)
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
        logIdCounter = 0
        addLog("Logs cleared", LogEntry.Level.DEBUG)
    }

    fun disconnectGateway() {
        viewModelScope.launch {
            addLog("Disconnecting gateway...", LogEntry.Level.WARN)
            gatewayRepository.disconnect()
        }
    }

    fun forceStateRefresh() {
        addLog("State: ${gatewayState.value.label}", LogEntry.Level.INFO)
        addLog("Session ID: ${diagnostics.value.sessionId ?: "N/A"}", LogEntry.Level.INFO)
        addLog("Sequence: ${diagnostics.value.lastSequenceNumber ?: "N/A"}", LogEntry.Level.INFO)
        addLog("Ping: ${diagnostics.value.heartbeatPing?.let { "${it}ms" } ?: "N/A"}", LogEntry.Level.INFO)
        addLog("Provider: ${diagnostics.value.currentAuthProvider ?: "N/A"}", LogEntry.Level.INFO)
    }
}
