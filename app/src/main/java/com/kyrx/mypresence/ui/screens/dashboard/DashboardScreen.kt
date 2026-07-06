package com.kyrx.mypresence.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyrx.mypresence.domain.model.GatewayConnectionState
import com.kyrx.mypresence.ui.animations.AnimatedScrollItem
import com.kyrx.mypresence.ui.animations.AuroraAnimation
import com.kyrx.mypresence.ui.animations.MagneticButton
import com.kyrx.mypresence.ui.components.PremiumCard
import com.kyrx.mypresence.ui.components.PremiumEmptyState
import com.kyrx.mypresence.ui.components.PremiumSwitch
import com.kyrx.mypresence.ui.components.ProfileCard
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Primary
import com.kyrx.mypresence.ui.theme.Secondary
import com.kyrx.mypresence.ui.theme.SurfaceBorder
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextSecondary
import com.kyrx.mypresence.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    var headerVisible by remember { mutableStateOf(false) }
    val gatewayState by viewModel.gatewayState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val presenceEnabled by viewModel.presenceEnabled.collectAsState()
    val presenceName by viewModel.presenceName.collectAsState()
    val presenceDetails by viewModel.presenceDetails.collectAsState()
    val presenceState by viewModel.presenceState.collectAsState()

    LaunchedEffect(Unit) { headerVisible = true }

    val scrollState = rememberScrollState()
    val username = currentUser?.username ?: "User"
    val discriminator = "#${currentUser?.discriminator ?: "0000"}"
    val isConnected = gatewayState is GatewayConnectionState.Connected

    Box(
        modifier = Modifier.fillMaxSize().background(Background)
    ) {
        AuroraAnimation(
            modifier = Modifier.fillMaxSize().blur(60.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            AnimatedVisibility(
                visible = headerVisible,
                enter = fadeIn(spring(dampingRatio = Spring.DampingRatioLowBouncy)) +
                        slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "My Presence",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Manage your Discord status",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                    MagneticButton {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = if (isConnected) listOf(Primary, Secondary) else listOf(SurfaceBorder, SurfaceBorder)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedScrollItem(index = 1) {
                ProfileCard(
                    username = username,
                    discriminator = discriminator,
                    isOnline = isConnected
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedScrollItem(index = 2) {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = if (presenceEnabled) listOf(Primary, Secondary) else listOf(SurfaceBorder, SurfaceBorder)
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Rich Presence",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = when (gatewayState) {
                                        is GatewayConnectionState.Connected -> "Active"
                                        is GatewayConnectionState.Connecting -> "Connecting..."
                                        is GatewayConnectionState.Disconnected -> "Inactive"
                                        is GatewayConnectionState.Error -> "Error"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isConnected) Primary else TextSecondary
                                )
                            }
                        }
                        PremiumSwitch(
                            checked = presenceEnabled,
                            onCheckedChange = { viewModel.togglePresence(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (presenceEnabled) {
                AnimatedScrollItem(index = 3) {
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                            Text(
                                text = "Presence Configuration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            PresenceTextField(
                                label = "Activity Name",
                                value = presenceName,
                                onValueChange = { viewModel.updatePresenceName(it) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            PresenceTextField(
                                label = "Details",
                                value = presenceDetails,
                                onValueChange = { viewModel.updatePresenceDetails(it) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            PresenceTextField(
                                label = "State",
                                value = presenceState,
                                onValueChange = { viewModel.updatePresenceState(it) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            AnimatedScrollItem(index = 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusCard(
                        icon = Icons.Filled.Gamepad,
                        title = "Status",
                        value = if (isConnected) "Connected" else "Offline",
                        modifier = Modifier.weight(1f)
                    )
                    StatusCard(
                        icon = Icons.Filled.Timer,
                        title = "Session",
                        value = if (isConnected) "Active" else "Idle",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedScrollItem(index = 5) {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Current Activity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        if (!presenceEnabled) {
                            PremiumEmptyState(
                                icon = Icons.Filled.Bolt,
                                title = "No Active Presence",
                                description = "Enable Rich Presence to show your status on Discord"
                            )
                        } else {
                            Text(
                                text = "$presenceName • $presenceDetails",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun PresenceTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = SurfaceBorder,
            focusedLabelColor = Primary,
            unfocusedLabelColor = TextSecondary,
            cursorColor = Primary,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}

@Composable
private fun StatusCard(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    PremiumCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}
