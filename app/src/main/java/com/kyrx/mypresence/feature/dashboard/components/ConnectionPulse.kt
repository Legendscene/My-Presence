package com.kyrx.mypresence.feature.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyrx.mypresence.core.gateway.GatewayConnectionState
import com.kyrx.mypresence.ui.components.GlassCard
import com.kyrx.mypresence.ui.theme.GlassAppearance
import com.kyrx.mypresence.ui.theme.Gold
import com.kyrx.mypresence.ui.theme.Success
import com.kyrx.mypresence.ui.theme.SurfaceBorder
import com.kyrx.mypresence.ui.theme.SurfaceCard
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextTertiary

@Composable
fun StatusCards(gatewayState: GatewayConnectionState) {
    val isConnected = gatewayState is GatewayConnectionState.Connected
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Sensors,
            label = "Gateway",
            value = if (isConnected) "Connected" else "Disconnected",
            valueColor = if (isConnected) Success else TextTertiary,
            iconBg = if (isConnected) Success.copy(alpha = 0.15f) else TextTertiary.copy(alpha = 0.1f)
        )
        StatusCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Bolt,
            label = "Session",
            value = if (isConnected) "Active" else "Idle",
            valueColor = if (isConnected) Gold else TextTertiary,
            iconBg = if (isConnected) Gold.copy(alpha = 0.15f) else TextTertiary.copy(alpha = 0.1f)
        )
    }
}

@Composable
private fun StatusCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    iconBg: Color = valueColor.copy(alpha = 0.12f)
) {
    GlassCard(
        modifier = modifier,
        appearance = GlassAppearance(
            baseColor = SurfaceCard, alpha = 0.55f,
            borderColor = SurfaceBorder.copy(alpha = 0.15f), cornerRadius = 16.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = valueColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.W500)
                Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.W700)
            }
        }
    }
}
