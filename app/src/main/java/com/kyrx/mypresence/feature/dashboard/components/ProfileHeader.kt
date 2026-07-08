package com.kyrx.mypresence.feature.dashboard.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kyrx.mypresence.core.gateway.GatewayConnectionState
import com.kyrx.mypresence.data.remote.DiscordUser
import com.kyrx.mypresence.ui.components.GlassCard
import com.kyrx.mypresence.ui.theme.GlassAppearance
import com.kyrx.mypresence.ui.theme.Gold
import com.kyrx.mypresence.ui.theme.Success
import com.kyrx.mypresence.ui.theme.SurfaceCard
import com.kyrx.mypresence.ui.theme.SurfaceLight
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextSecondary
import com.kyrx.mypresence.ui.theme.TextTertiary

@Composable
fun ProfileHeader(
    user: DiscordUser?,
    gatewayState: GatewayConnectionState,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val globalName = user?.global_name ?: user?.username ?: "User"
    val username = user?.username ?: "User"
    val avatarUrl = user?.let {
        if (it.avatar != null) {
            val ext = if (it.avatar.startsWith("a_")) "gif" else "png"
            "https://cdn.discordapp.com/avatars/${it.id}/${it.avatar}.$ext"
        } else {
            val index = try { it.discriminator.toInt() % 5 } catch (_: Exception) { 0 }
            "https://cdn.discordapp.com/embed/avatars/$index.png"
        }
    }
    val bannerUrl = user?.let {
        it.banner?.let { b ->
            val ext = if (b.startsWith("a_")) "gif" else "png"
            "https://cdn.discordapp.com/banners/${it.id}/$b.$ext"
        }
    }

    val isConnected = gatewayState is GatewayConnectionState.Connected

    GlassCard(
        appearance = GlassAppearance(
            baseColor = SurfaceCard, alpha = 0.65f,
            borderColor = Gold.copy(alpha = 0.15f),
            glowColor = Gold.copy(alpha = 0.06f), cornerRadius = 20.dp
        )
    ) {
        Column {
            if (bannerUrl != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(SurfaceLight)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(bannerUrl).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, SurfaceCard.copy(alpha = 0.9f)),
                                startY = 0f, endY = 800f
                            )
                        )
                    )
                }
            }
            Row(
                modifier = Modifier.padding(
                    start = 16.dp, end = 16.dp,
                    top = if (bannerUrl != null) (-28).dp else 16.dp, bottom = 16.dp
                ),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(SurfaceLight),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(avatarUrl).crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = username.firstOrNull()?.uppercase() ?: "?",
                            color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.W700
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(globalName, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.W700)
                    Text("@$username", color = TextSecondary, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isConnected) Success.copy(alpha = 0.15f)
                                else TextTertiary.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            if (isConnected) "Active" else "Disconnected",
                            color = if (isConnected) Success else TextTertiary,
                            fontSize = 12.sp, fontWeight = FontWeight.W600
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    IconButton(onClick = onLogout, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
