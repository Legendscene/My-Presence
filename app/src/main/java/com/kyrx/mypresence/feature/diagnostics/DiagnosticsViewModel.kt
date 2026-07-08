package com.kyrx.mypresence.feature.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyrx.mypresence.core.gateway.GatewayConnectionState
import com.kyrx.mypresence.data.remote.DiscordGateway
import com.kyrx.mypresence.domain.usecase.TogglePresenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val discordGateway: DiscordGateway,
    private val togglePresence: TogglePresenceUseCase
) : ViewModel() {

    val gatewayState: StateFlow<GatewayConnectionState> = discordGateway.state

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private var logIdCounter = 0L

    init {
        addLog("ViewModel initialized", LogEntry.Level.DEBUG)
        addLog("Gateway state: ${gatewayState.value}", LogEntry.Level.INFO)
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
            discordGateway.disconnect()
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
            discordGateway.disconnect()
        }
    }
}
