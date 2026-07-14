package com.kyrx.mypresence.feature.diagnostics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyrx.mypresence.core.gateway.GatewayConnectionState
import com.kyrx.mypresence.ui.theme.Accent
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Dimens
import com.kyrx.mypresence.ui.theme.Error
import com.kyrx.mypresence.ui.theme.Success
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextSecondary
import com.kyrx.mypresence.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    vm: DiagnosticsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val gatewayState by vm.gatewayState.collectAsStateWithLifecycle()
    val diagnostics by vm.diagnostics.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Diagnostics",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.lg)
                .verticalScroll(rememberScrollState())
        ) {
            val gs = gatewayState
            SectionHeader("Connection")
            DiagnosticRow("State", stateLabel(gs))
            if (gs is GatewayConnectionState.Unauthorized) {
                DiagnosticRow("Error Message", gs.message)
            }
            if (gs is GatewayConnectionState.FatalError) {
                DiagnosticRow("Error Message", gs.message)
            }
            if (gs is GatewayConnectionState.InvalidSession) {
                DiagnosticRow("Can Resume", gs.canResume.toString())
            }
            DiagnosticRow("Gateway Version", "v${diagnostics.gatewayVersion}")
            DiagnosticRow("Heartbeat Ping", diagnostics.heartbeatPing?.let { "${it}ms" } ?: "N/A")
            DiagnosticRow("Heartbeat ACKs", diagnostics.heartbeatAckCount.toString())
            DiagnosticRow("Session ID", diagnostics.sessionId?.take(16)?.plus("...") ?: "N/A")
            DiagnosticRow("Sequence Number", diagnostics.lastSequenceNumber?.toString() ?: "N/A")

            HorizontalDivider(color = TextTertiary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = Dimens.md))

            SectionHeader("Authentication")
            DiagnosticRow("Auth Provider", diagnostics.currentAuthProvider ?: "N/A")
            DiagnosticRow("READY received", if (diagnostics.readyReceived) "Yes" else "No")
            DiagnosticRow("Auth Milestone", diagnostics.authMilestone ?: "N/A")
            DiagnosticRow("Last Close Code", diagnostics.lastCloseCode?.toString() ?: "N/A")
            DiagnosticRow("Last Close Reason", diagnostics.lastCloseReason?.take(80) ?: "N/A")

            HorizontalDivider(color = TextTertiary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = Dimens.md))

            SectionHeader("Presence")
            DiagnosticRow("Presence Updates", diagnostics.presenceUpdateCount.toString())
            DiagnosticRow("Last Dispatch", diagnostics.lastGatewayDispatch ?: "N/A")
            DiagnosticRow("OP3 Accepted", when {
                diagnostics.lastOp3Accepted -> "Yes"
                diagnostics.lastOp3Timestamp != null -> "Pending"
                else -> "N/A"
            })

            HorizontalDivider(color = TextTertiary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = Dimens.md))

            SectionHeader("State")
            DiagnosticRow("Reconnect Attempts", diagnostics.reconnectAttempts.toString())
            DiagnosticRow("Foreground Service", if (diagnostics.foregroundServiceActive) "Active" else "Inactive")
            DiagnosticRow("Usage Stats", if (diagnostics.hasUsageStatsPermission) "Granted" else "Denied")
            DiagnosticRow("Battery Opt.", if (diagnostics.isIgnoringBatteryOptimizations) "Disabled" else "Enabled")

            if (diagnostics.eventLog.isNotEmpty()) {
                HorizontalDivider(color = TextTertiary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = Dimens.md))
                SectionHeader("Gateway Events (last ${diagnostics.eventLog.size})")
                for ((i, entry) in diagnostics.eventLog.takeLast(20).withIndex()) {
                    val entryColor = when {
                        entry.contains("4004") || entry.contains("FAILURE") || entry.contains("CLOSE") -> Error
                        entry.contains("READY") || entry.contains("HEARTBEAT ACK") -> Success
                        entry.contains("IDENTIFY") || entry.contains("HELLO") -> Accent
                        else -> TextTertiary
                    }
                    Text(
                        text = "#${diagnostics.eventLog.size - diagnostics.eventLog.takeLast(20).size + i + 1} $entry",
                        color = entryColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }

            Spacer(Modifier.height(Dimens.xxxl))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = Accent,
        modifier = Modifier.padding(bottom = Dimens.sm)
    )
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary
        )
    }
}

private fun stateLabel(state: GatewayConnectionState): String = when (state) {
    is GatewayConnectionState.Connected -> "Connected"
    is GatewayConnectionState.Connecting -> "Connecting"
    is GatewayConnectionState.Authenticating -> "Authenticating"
    is GatewayConnectionState.Resuming -> "Resuming"
    is GatewayConnectionState.Reconnecting -> "Reconnecting"
    is GatewayConnectionState.Disconnected -> "Disconnected"
    is GatewayConnectionState.HeartbeatLost -> "Heartbeat Lost"
    is GatewayConnectionState.InvalidSession -> "Invalid Session"
    is GatewayConnectionState.Unauthorized -> "Unauthorized"
    is GatewayConnectionState.FatalError -> "Fatal Error"
    is GatewayConnectionState.RateLimited -> "Rate Limited"
}
