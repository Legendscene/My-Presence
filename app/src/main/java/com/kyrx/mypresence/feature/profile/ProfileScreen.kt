package com.kyrx.mypresence.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyrx.mypresence.domain.repository.AuthState
import com.kyrx.mypresence.ui.animation.StaggeredReveal
import com.kyrx.mypresence.ui.components.GlassCard
import com.kyrx.mypresence.ui.components.GradientText
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Blurple
import com.kyrx.mypresence.ui.theme.GlassAppearance
import com.kyrx.mypresence.ui.theme.Gold
import com.kyrx.mypresence.ui.theme.GoldLight
import com.kyrx.mypresence.ui.theme.SurfaceBorder
import com.kyrx.mypresence.ui.theme.SurfaceCard
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextSecondary
import com.kyrx.mypresence.ui.theme.TextTertiary
import com.kyrx.mypresence.feature.profile.ProfileViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.platform.LocalContext

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val username = currentUser?.username ?: "User"
    val global_name = currentUser?.global_name ?: username
    val discriminator = currentUser?.discriminator ?: "0000"
    val avatarUrl = currentUser?.let { user ->
        if (user.avatar != null) {
            val ext = if (user.avatar.startsWith("a_")) "gif" else "png"
            "https://cdn.discordapp.com/avatars/${user.id}/${user.avatar}.$ext?size=256"
        } else {
            val index = try { user.discriminator.toInt() % 5 } catch (_: Exception) { 0 }
            "https://cdn.discordapp.com/embed/avatars/$index.png"
        }
    }
    val bannerUrl = currentUser?.let { user ->
        user.banner?.let {
            val ext = if (it.startsWith("a_")) "gif" else "png"
            "https://cdn.discordapp.com/banners/${user.id}/$it.$ext?size=512"
        }
    }
    val decorationUrl = currentUser?.let { user ->
        user.avatar_decoration_data?.asset?.takeIf { it.isNotBlank() }?.let {
            "https://cdn.discordapp.com/avatar-decoration-presets/$it.png?size=256"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        StaggeredReveal(0) {
            GradientText(
                text = "Profile",
                fontSize = 28.sp,
                fontWeight = FontWeight.W700
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        StaggeredReveal(1) {
            GlassCard(
                appearance = GlassAppearance(
                    baseColor = SurfaceCard,
                    alpha = 0.65f,
                    borderColor = Gold.copy(alpha = 0.15f),
                    glowColor = Gold.copy(alpha = 0.06f),
                    cornerRadius = 20.dp
                )
            ) {
                Column {
                    if (bannerUrl != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(104.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Blurple.copy(alpha = 0.18f))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(bannerUrl).crossfade(true).build(),
                                contentDescription = "Discord banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(76.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(GoldLight, Gold)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarUrl != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(avatarUrl).crossfade(true).build(),
                                        contentDescription = "Discord avatar",
                                        modifier = Modifier.size(68.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = username.firstOrNull()?.uppercase() ?: "?",
                                        color = Background,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.W700
                                    )
                                }
                            }
                            if (decorationUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(decorationUrl).crossfade(true).build(),
                                    contentDescription = "Avatar decoration",
                                    modifier = Modifier.size(76.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = global_name,
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.W600
                            )
                            Text(
                                text = if (discriminator == "0") "@$username" else "$username#$discriminator",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Gold.copy(alpha = 0.12f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit",
                                tint = Gold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        StaggeredReveal(2) {
            GlassCard(
                appearance = GlassAppearance(
                    baseColor = SurfaceCard,
                    alpha = 0.6f,
                    borderColor = SurfaceBorder.copy(alpha = 0.2f),
                    cornerRadius = 18.dp
                )
            ) {
                ProfileTile(
                    icon = Icons.Filled.Forum,
                    iconColor = Blurple,
                    title = "Connected Account",
                    subtitle = "Discord — $global_name",
                    showArrow = false
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        StaggeredReveal(3) {
            GlassCard(
                appearance = GlassAppearance(
                    baseColor = SurfaceCard,
                    alpha = 0.6f,
                    borderColor = SurfaceBorder.copy(alpha = 0.2f),
                    cornerRadius = 18.dp
                )
            ) {
                ProfileTile(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    iconColor = com.kyrx.mypresence.ui.theme.Error,
                    title = "Sign Out",
                    subtitle = "Disconnect from Discord",
                    onClick = { viewModel.signOut() }
                )
            }
        }
    }
}

@Composable
private fun ProfileTile(
    icon: ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    showArrow: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ) else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.W600
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
