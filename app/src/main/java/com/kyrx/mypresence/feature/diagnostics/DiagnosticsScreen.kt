package com.kyrx.mypresence.feature.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyrx.mypresence.core.gateway.GatewayConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    vm: DiagnosticsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val gatewayState by vm.gatewayState.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                actions = {
                    TextButton(onClick = { vm.clearLogs() }) { Text("Clear") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = listState,
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                ConnectionStatusCard(gatewayState)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { vm.testGatewayConnection() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test Gateway", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { vm.forceReidentify() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Re-Identify", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { vm.disconnectGateway() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Disconnect", maxLines = 1)
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Event Log (${logs.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(logs, key = { it.id }) { log ->
                LogEntryRow(log)
            }

            if (logs.isEmpty()) {
                item {
                    Text(
                        text = "No events captured yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun ConnectionStatusCard(state: GatewayConnectionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Gateway Status", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            val (label, color) = when (state) {
                is GatewayConnectionState.Connected -> "Connected" to Color(0xFF43B581)
                is GatewayConnectionState.Connecting -> "Connecting..." to Color(0xFFFAA61A)
                is GatewayConnectionState.Disconnected -> "Disconnected" to Color(0xFF747F8D)
                is GatewayConnectionState.Error -> "Error: ${state.message}" to Color(0xFFED4245)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(color)
                )
                Spacer(Modifier.width(8.dp))
                Text(label, fontWeight = FontWeight.Medium)
            }

            if (state is GatewayConnectionState.Connected) {
                Text(
                    text = "Session established",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    val color = when (entry.level) {
        LogEntry.Level.ERROR -> MaterialTheme.colorScheme.error
        LogEntry.Level.WARN -> Color(0xFFFAA61A)
        LogEntry.Level.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
        LogEntry.Level.DEBUG -> Color(0xFF5865F2)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = entry.formattedTime,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = entry.message,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = color,
            modifier = Modifier.weight(1f)
        )
    }
}
