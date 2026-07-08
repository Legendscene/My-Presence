package com.kyrx.mypresence.feature.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyrx.mypresence.domain.model.AppInfo
import com.kyrx.mypresence.domain.model.AppPresenceConfig
import com.kyrx.mypresence.ui.components.PremiumSwitch
import com.kyrx.mypresence.ui.theme.Cyan
import com.kyrx.mypresence.ui.theme.Success
import com.kyrx.mypresence.ui.theme.SurfaceBorder
import com.kyrx.mypresence.ui.theme.SurfaceCard
import com.kyrx.mypresence.ui.theme.SurfaceLight
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextTertiary

@Composable
fun AppTracker(
    installedApps: List<AppInfo>,
    enabledApps: Set<String>,
    detectedApp: AppInfo?,
    appConfigs: List<AppPresenceConfig>,
    onToggleApp: (String, Boolean) -> Unit,
    onEditApp: (AppInfo) -> Unit,
    hasUsageStatsPermission: Boolean,
    requestUsageAccess: () -> Unit,
    previewPresence: (AppInfo) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Cyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = null,
                        tint = Cyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Tracked Apps", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.W600)
                Spacer(modifier = Modifier.weight(1f))
                if (detectedApp != null) {
                    Text(detectedApp.appName, color = Success, fontSize = 12.sp, fontWeight = FontWeight.W500)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            if (installedApps.isEmpty()) {
                Text("Scanning installed apps...", color = TextTertiary, fontSize = 13.sp)
            } else {
                installedApps.take(24).forEachIndexed { index, app ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = SurfaceBorder.copy(alpha = 0.2f),
                            thickness = 0.5.dp
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (app.packageName in enabledApps)
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { onEditApp(app) }
                                    ) else Modifier
                            )
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.appName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W500)
                            Text(
                                previewPresence(app),
                                color = TextTertiary, fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        PremiumSwitch(
                            checked = app.packageName in enabledApps,
                            onCheckedChange = {
                                if (it && !hasUsageStatsPermission) {
                                    requestUsageAccess()
                                } else {
                                    onToggleApp(app.packageName, it)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
