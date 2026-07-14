package com.kyrx.mypresence.feature.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kyrx.mypresence.core.gateway.GatewayConnectionState
import com.kyrx.mypresence.core.gateway.GatewayDiagnostics
import com.kyrx.mypresence.data.remote.DiscordUser
import com.kyrx.mypresence.domain.model.AppInfo
import com.kyrx.mypresence.domain.repository.AuthState
import com.kyrx.mypresence.ui.components.GlassCard
import com.kyrx.mypresence.ui.theme.*

@Composable
fun DashboardScreen(
    vm: DashboardViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToInstalledApps: () -> Unit = {},
    onNavigateToPresenceEditor: () -> Unit = {},
    onNavigateToCustomPresets: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val authState by vm.authState.collectAsStateWithLifecycle()
    val gatewayState by vm.gatewayState.collectAsStateWithLifecycle()
    val diagnostics by vm.diagnostics.collectAsStateWithLifecycle()
    val presenceEnabled by vm.presenceEnabled.collectAsStateWithLifecycle()
    val detectedApp by vm.detectedApp.collectAsStateWithLifecycle()

    val user = (authState as? AuthState.Authenticated)?.user
    var showLogoutConfirm by remember { mutableStateOf(false) }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Disconnect Discord?") },
            text = { Text("Your presence will stop updating until you log in again.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    vm.logout()
                    onLogout()
                }) { Text("Disconnect", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Dimens.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(Dimens.md)
    ) {
        item {
            GradientHeader(
                user = user,
                gatewayState = gatewayState,
                onProfileClick = onNavigateToProfile
            )
        }

        item {
            ConnectionStatusCard(diagnostics = diagnostics)
        }

        item {
            PresenceToggleCard(
                enabled = presenceEnabled,
                detectedApp = detectedApp,
                onToggle = { vm.togglePresenceEnabled(it) }
            )
        }

        item {
            ActivityPreviewCard(
                activityJson = diagnostics.lastActivityJson,
                onEditPresence = onNavigateToPresenceEditor
            )
        }

        item {
            QuickActionsGrid(
                onPresenceEditor = onNavigateToPresenceEditor,
                onAppDetection = onNavigateToInstalledApps,
                onDiagnostics = onNavigateToDiagnostics,
                onSettings = onNavigateToSettings,
                onCustomPresets = onNavigateToCustomPresets,
                onAbout = onNavigateToAbout
            )
        }

        item {
            AppDetectionCard(
                detectedApp = detectedApp,
                hasUsageStatsPermission = vm.hasUsageStatsPermission(),
                onViewAllApps = onNavigateToInstalledApps,
                onRequestUsageAccess = {
                    context.startActivity(vm.requestUsageAccessIntent())
                }
            )
        }

        item {
            Spacer(Modifier.height(Dimens.sm))
            TextButton(
                onClick = { showLogoutConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Disconnect Discord", color = Error, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(Dimens.screenVertical))
        }
    }
}

@Composable
private fun GradientHeader(
    user: DiscordUser?,
    gatewayState: GatewayConnectionState,
    onProfileClick: () -> Unit
) {
    val displayName = user?.let { it.global_name ?: it.username } ?: "My Presence"
    val username = user?.username ?: "user"
    val discriminator = user?.discriminator ?: "0"

    val avatarUrl = user?.let {
        val hash = it.avatar ?: return@let null
        val ext = if (hash.startsWith("a_")) "gif" else "png"
        "https://cdn.discordapp.com/avatars/${it.id}/$hash.$ext?size=128"
    }

    val bannerUrl = user?.let {
        it.banner?.let { b ->
            val ext = if (b.startsWith("a_")) "gif" else "png"
            "https://cdn.discordapp.com/banners/${it.id}/$b.$ext?size=480"
        }
    }

    val decorationUrl = user?.let {
        it.avatar_decoration_data?.asset?.takeIf { a -> a.isNotBlank() }?.let { a ->
            "https://cdn.discordapp.com/avatar-decorations/${it.id}/$a.png"
        }
    }

    val bannerColor = user?.banner_color
    val baseAccent = remember(bannerColor, user?.accent_color) {
        if (bannerColor != null) {
            try {
                val rgb = bannerColor.removePrefix("#").toInt(16)
                Color(rgb or 0xFF000000.toInt())
            } catch (_: Exception) { Accent }
        } else user?.accent_color?.let { Color(it) } ?: Accent
    }

    val premiumType = user?.premium_type
    val publicFlags = user?.public_flags
    val badges = remember(publicFlags) {
        if (publicFlags == null) emptyList()
        else decodeBadges(publicFlags)
    }

    val dotColor = gatewayState.statusColor

    val infiniteTransition = rememberInfiniteTransition()
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.cardCorner))
            .background(Surface)
            .clickable(onClick = onProfileClick)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 84.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(
                        Brush.horizontalGradient(colors = listOf(baseAccent, baseAccent.copy(alpha = 0.6f)))
                    )
            ) {
                if (bannerUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(bannerUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Surface.copy(alpha = 0.9f))
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = Dimens.md, y = 28.dp)
                    .size(56.dp)
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Avatar",
                        modifier = Modifier.size(56.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(28.dp))
                    }
                }
                if (decorationUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(decorationUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Decoration",
                        modifier = Modifier.size(72.dp).offset(x = (-8).dp, y = (-8).dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-8).dp, y = (-4).dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .alpha(dotAlpha)
            )
        }

        Column(modifier = Modifier.padding(horizontal = Dimens.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W700),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                if (premiumType != null && premiumType > 0) {
                    val label = when (premiumType) { 1 -> "Classic"; 2 -> ""; 3 -> "Basic"; else -> "" }
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Nitro $label",
                        tint = Color(0xFF9B59B6),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            val usernameDisplay = if (discriminator == "0") "@$username" else "$username#$discriminator"
            Text(
                text = usernameDisplay,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            if (badges.isNotEmpty() || (premiumType != null && premiumType > 0)) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (premiumType != null && premiumType > 0) {
                        val label = when (premiumType) { 1 -> "Nitro Classic"; 2 -> "Nitro"; 3 -> "Nitro Basic"; else -> "Nitro" }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.horizontalGradient(colors = listOf(Color(0xFF9B59B6), Color(0xFFFF6B9D), Color(0xFFFFA07A))))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(text = label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.W700)
                        }
                    }
                    badges.forEach { (name, color, iconChar) ->
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(color.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = iconChar, color = color, fontSize = 10.sp, fontWeight = FontWeight.W700)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(Dimens.md))
    }
}

@Composable
private fun ConnectionStatusCard(diagnostics: GatewayDiagnostics) {
    val isConnected = diagnostics.state is GatewayConnectionState.Connected
    val isActive = isConnected || diagnostics.state is GatewayConnectionState.Authenticating || diagnostics.state is GatewayConnectionState.Connecting

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Dimens.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
            Text(
                text = "Connection",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            MetricRow(
                label = "Gateway",
                value = diagnostics.state.label,
                valueColor = diagnostics.state.statusColor
            )
            if (diagnostics.authMilestone != null) {
                MetricRow(
                    label = "Auth Milestone",
                    value = diagnostics.authMilestone,
                    valueColor = TextSecondary
                )
            }
            MetricRow(
                label = "Gateway Version",
                value = "v${diagnostics.gatewayVersion}"
            )
            MetricRow(
                label = "Ping",
                value = if (diagnostics.heartbeatPing != null) "${diagnostics.heartbeatPing}ms" else if (isConnected) "\u2014" else "\u2014"
            )
            MetricRow(
                label = "Session ID",
                value = diagnostics.sessionId?.takeLast(10) ?: "\u2014"
            )
            MetricRow(
                label = "Sequence",
                value = diagnostics.lastSequenceNumber?.toString() ?: "\u2014"
            )
            MetricRow(
                label = "Last Dispatch",
                value = diagnostics.lastGatewayDispatch ?: "\u2014"
            )
            MetricRow(
                label = "Heartbeats",
                value = if (diagnostics.heartbeatsSent > 0 || diagnostics.heartbeatAckCount > 0)
                    "${diagnostics.heartbeatsSent} sent / ${diagnostics.heartbeatAckCount} ack"
                else "\u2014"
            )
            if (diagnostics.connectionStartedAt != null) {
                val elapsed = (System.currentTimeMillis() - diagnostics.connectionStartedAt) / 1000
                val min = elapsed / 60
                val sec = elapsed % 60
                MetricRow(
                    label = "Connected For",
                    value = if (isConnected) "${min}m ${sec}s" else "${elapsed}s",
                    valueColor = if (isConnected) Success else TextSecondary
                )
            }
            if (diagnostics.reconnectAttempts > 0) {
                MetricRow(
                    label = "Reconnects",
                    value = "${diagnostics.reconnectAttempts}"
                )
            }
            MetricRow(
                label = "Presence Updates",
                value = "${diagnostics.presenceUpdateCount}"
            )
            if (diagnostics.lastCloseCode != null) {
                MetricRow(
                    label = "Last Close",
                    value = "${diagnostics.lastCloseCode}: ${diagnostics.lastCloseReason?.take(30) ?: "\u2014"}",
                    valueColor = Error
                )
            }
        }
    }
}

@Composable
private fun PresenceToggleCard(
    enabled: Boolean,
    detectedApp: AppInfo?,
    onToggle: (Boolean) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Dimens.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Rich Presence",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    if (detectedApp != null) {
                        Text(
                            text = detectedApp.appName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Accent,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = SurfaceBorder
                    )
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) color.copy(alpha = 0.15f) else Surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.md, vertical = Dimens.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(Dimens.sm))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) color else TextSecondary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ActivityPreviewCard(
    activityJson: String?,
    onEditPresence: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Dimens.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Activity",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                TextButton(onClick = onEditPresence) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSmall)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Edit Presence", style = MaterialTheme.typography.labelSmall)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp, max = 120.dp)
                    .clip(RoundedCornerShape(Dimens.sm))
                    .background(SurfaceElevated)
                    .padding(Dimens.sm)
            ) {
                Text(
                    text = activityJson ?: "No current activity data",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = if (activityJson != null) TextPrimary else TextDisabled,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(
    onPresenceEditor: () -> Unit,
    onAppDetection: () -> Unit,
    onDiagnostics: () -> Unit,
    onSettings: () -> Unit,
    onCustomPresets: () -> Unit,
    onAbout: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
        ) {
            QuickActionTile(
                icon = Icons.Filled.Edit,
                label = "Presence Editor",
                onClick = onPresenceEditor,
                modifier = Modifier.weight(1f)
            )
            QuickActionTile(
                icon = Icons.Filled.Apps,
                label = "App Detection",
                onClick = onAppDetection,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
        ) {
            QuickActionTile(
                icon = Icons.Filled.BugReport,
                label = "Diagnostics",
                onClick = onDiagnostics,
                modifier = Modifier.weight(1f)
            )
            QuickActionTile(
                icon = Icons.Filled.Settings,
                label = "Settings",
                onClick = onSettings,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
        ) {
            QuickActionTile(
                icon = Icons.Filled.Palette,
                label = "Custom Presets",
                onClick = onCustomPresets,
                modifier = Modifier.weight(1f)
            )
            QuickActionTile(
                icon = Icons.Filled.Info,
                label = "About",
                onClick = onAbout,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.clickable(onClick = onClick),
        contentPadding = PaddingValues(vertical = Dimens.md)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextSecondary,
                modifier = Modifier.size(Dimens.iconLarge)
            )
            Spacer(Modifier.height(Dimens.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AppDetectionCard(
    detectedApp: AppInfo?,
    hasUsageStatsPermission: Boolean,
    onViewAllApps: () -> Unit,
    onRequestUsageAccess: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Dimens.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Detection",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = detectedApp?.appName ?: "No app detected",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (detectedApp != null) TextSecondary else TextDisabled
                    )
                }
                TextButton(onClick = onViewAllApps) {
                    Text("View All", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (!hasUsageStatsPermission) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRequestUsageAccess),
                    shape = RoundedCornerShape(Dimens.sm),
                    color = Warning.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(Dimens.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "\u26A0",
                            style = MaterialTheme.typography.labelMedium,
                            color = Warning
                        )
                        Spacer(Modifier.width(Dimens.sm))
                        Text(
                            text = "Usage stats permission required for app detection",
                            style = MaterialTheme.typography.bodySmall,
                            color = Warning,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}

private val GatewayConnectionState.statusColor: Color
    get() = when (this) {
        is GatewayConnectionState.Connected -> Success
        is GatewayConnectionState.Connecting,
        is GatewayConnectionState.Authenticating,
        is GatewayConnectionState.Resuming,
        is GatewayConnectionState.Reconnecting -> Warning
        is GatewayConnectionState.HeartbeatLost,
        is GatewayConnectionState.Disconnected,
        is GatewayConnectionState.RateLimited,
        is GatewayConnectionState.InvalidSession,
        is GatewayConnectionState.Unauthorized,
        is GatewayConnectionState.FatalError -> Error
    }

private fun decodeBadges(flags: Int): List<Triple<String, Color, String>> {
    val badgeList = mutableListOf<Triple<String, Color, String>>()
    if (flags and (1 shl 0) != 0) badgeList.add(Triple("Staff", Color(0xFF5865F2), "S"))
    if (flags and (1 shl 1) != 0) badgeList.add(Triple("Partner", Color(0xFF57F287), "P"))
    if (flags and (1 shl 2) != 0) badgeList.add(Triple("Hypesquad", Color(0xFFFEE75C), "H"))
    if (flags and (1 shl 3) != 0) badgeList.add(Triple("Bug Hunter", Color(0xFFFEE75C), "B"))
    if (flags and (1 shl 6) != 0) badgeList.add(Triple("Bravery", Color(0xFF5865F2), "Br"))
    if (flags and (1 shl 7) != 0) badgeList.add(Triple("Brilliance", Color(0xFF57F287), "Bl"))
    if (flags and (1 shl 8) != 0) badgeList.add(Triple("Balance", Color(0xFFFEE75C), "Ba"))
    if (flags and (1 shl 9) != 0) badgeList.add(Triple("Early Supporter", Color(0xFFFEE75C), "ES"))
    if (flags and (1 shl 14) != 0) badgeList.add(Triple("Bug Hunter 2", Color(0xFFFEE75C), "BH"))
    if (flags and (1 shl 17) != 0) badgeList.add(Triple("Developer", Color(0xFF57F287), "D"))
    if (flags and (1 shl 18) != 0) badgeList.add(Triple("Mod", Color(0xFF5865F2), "M"))
    if (flags and (1 shl 22) != 0) badgeList.add(Triple("Active Developer", Color(0xFF5865F2), "AD"))
    return badgeList
}
