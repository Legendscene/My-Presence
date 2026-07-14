package com.kyrx.mypresence.feature.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kyrx.mypresence.ui.components.GlassCard
import com.kyrx.mypresence.ui.components.GradientText
import com.kyrx.mypresence.ui.components.ShimmerCard
import com.kyrx.mypresence.ui.theme.Accent
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Dimens
import com.kyrx.mypresence.ui.theme.Error
import com.kyrx.mypresence.ui.theme.Success
import com.kyrx.mypresence.ui.theme.Surface
import com.kyrx.mypresence.ui.theme.SurfaceElevated
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextSecondary
import com.kyrx.mypresence.ui.theme.TextTertiary
import com.kyrx.mypresence.ui.theme.Warning

@Suppress("DEPRECATION")
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading = currentUser == null

    val user = currentUser
    val username = user?.username ?: ""
    val displayName = user?.let { it.global_name ?: it.username } ?: ""
    val discriminator = user?.discriminator ?: "0"
    val pronouns = user?.pronouns

    val avatarUrl = user?.let { u ->
        if (u.avatar != null) {
            val ext = if (u.avatar.startsWith("a_")) "gif" else "png"
            "https://cdn.discordapp.com/avatars/${u.id}/${u.avatar}.$ext?size=256"
        } else {
            val index = try { (u.id.toLong() shr 22) % 6 } catch (_: Exception) { 0 }
            "https://cdn.discordapp.com/embed/avatars/${index}.png"
        }
    }

    val bannerUrl = user?.let { u ->
        u.banner?.let {
            val ext = if (it.startsWith("a_")) "gif" else "png"
            "https://cdn.discordapp.com/banners/${u.id}/$it.$ext?size=600"
        }
    }

    val decorationUrl = user?.let { u ->
        u.avatar_decoration_data?.asset?.takeIf { it.isNotBlank() }?.let {
            "https://cdn.discordapp.com/avatar-decorations/${u.id}/$it.png"
        }
    }

    val premiumType = user?.premium_type
    val publicFlags = user?.public_flags
    val nameplateColor = user?.nameplate?.let { Color(it) }

    val bannerBrush = remember(user) {
        val bc = user?.banner_color
        val ac = user?.accent_color
        when {
            bc != null -> {
                try {
                    val rgb = bc.removePrefix("#").toInt(16)
                    Brush.verticalGradient(
                        colors = listOf(Color(rgb or 0xFF000000.toInt()), Surface)
                    )
                } catch (_: Exception) {
                    Brush.verticalGradient(colors = listOf(Accent, Surface))
                }
            }
            ac != null -> Brush.verticalGradient(colors = listOf(Color(ac), Surface))
            else -> Brush.verticalGradient(colors = listOf(Accent, Surface))
        }
    }

    val badges = remember(publicFlags) {
        if (publicFlags == null) emptyList()
        else decodeBadges(publicFlags)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.screenHorizontal, vertical = Dimens.screenVertical)
    ) {
        GradientText(text = "Profile", fontSize = 28.sp, fontWeight = FontWeight.W700)

        Spacer(modifier = Modifier.height(Dimens.lg))

        if (isLoading) {
            ShimmerCard(height = 180.dp)
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerCard(height = 60.dp)
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerCard(height = 100.dp)
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerCard(height = 120.dp)
        } else {
            GlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(bannerBrush)
                        ) {
                            if (bannerUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(bannerUrl).crossfade(true).build(),
                                    contentDescription = "Banner",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Surface.copy(alpha = 0.85f))
                                        )
                                    )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = 44.dp)
                                .size(88.dp)
                        ) {
                            val avatarModifier = if (nameplateColor != null) {
                                Modifier.size(88.dp).clip(CircleShape).background(nameplateColor)
                                    .padding(3.dp).clip(CircleShape).background(SurfaceElevated)
                            } else {
                                Modifier.size(88.dp).clip(CircleShape).background(SurfaceElevated)
                            }
                            Box(modifier = avatarModifier,
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarUrl != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(avatarUrl).crossfade(true).build(),
                                        contentDescription = "Avatar",
                                        modifier = Modifier.size(82.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            if (decorationUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(decorationUrl).crossfade(true).build(),
                                    contentDescription = "Decoration",
                                    modifier = Modifier.size(110.dp).offset(x = (-11).dp, y = (-11).dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(52.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = displayName,
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.W700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (premiumType != null && premiumType > 0) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = "Nitro",
                                    tint = Color(0xFF9B59B6),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (!pronouns.isNullOrBlank()) {
                            Text(
                                text = pronouns,
                                color = TextSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(2.dp))
                        }

                        val usernameDisplay = if (discriminator == "0") "@$username" else "$username#$discriminator"
                        Text(
                            text = usernameDisplay,
                            color = TextTertiary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        if (badges.isNotEmpty() || (premiumType != null && premiumType > 0)) {
                            Spacer(Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (premiumType != null && premiumType > 0) {
                                    val label = when (premiumType) {
                                        1 -> "Nitro Classic"; 2 -> "Nitro"; 3 -> "Nitro Basic"
                                        else -> "Nitro"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF9B59B6), Color(0xFFFF6B9D), Color(0xFFFFA07A))
                                                )
                                            )
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = label,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.W700
                                            )
                                        }
                                    }
                                }
                                badges.forEach { (name, color, iconChar) ->
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(color.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = iconChar,
                                            color = color,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.W700
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(Dimens.lg))

                    if (!user?.bio.isNullOrBlank()) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.lg),
                            contentPadding = PaddingValues(Dimens.lg)
                        ) {
                        Text(
                            text = "ABOUT ME",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W700,
                            letterSpacing = 1.sp
                        )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = user?.bio ?: "",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                        Spacer(Modifier.height(Dimens.sm))
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.lg),
                        contentPadding = PaddingValues(Dimens.lg)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(10.dp).clip(CircleShape).background(Success)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Connected Account",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.W600
                                )
                                Text(
                                    text = "Discord — $displayName",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Success,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(Dimens.sm))

                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.lg),
                        contentPadding = PaddingValues(Dimens.lg)
                    ) {
                        Text(
                            text = "ACCOUNT DETAILS",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W700,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        ProfileDetailRow(label = "User ID", value = user?.id ?: "", onCopy = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(user?.id ?: ""))
                        })
                        if (!user?.email.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            ProfileDetailRow(label = "Email", value = user?.email ?: "", onCopy = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(user?.email ?: ""))
                            })
                        }
                        if (user?.locale != null) {
                            Spacer(Modifier.height(8.dp))
                            ProfileDetailRow(label = "Locale", value = user.locale, onCopy = {})
                        }
                        if (user?.verified != null) {
                            Spacer(Modifier.height(8.dp))
                            ProfileDetailRow(label = "Verified", value = if (user.verified) "Yes" else "No", onCopy = {})
                        }
                        if (user?.mfa_enabled != null) {
                            Spacer(Modifier.height(8.dp))
                            ProfileDetailRow(label = "MFA Enabled", value = if (user.mfa_enabled) "Yes" else "No", onCopy = {})
                        }
                    }

                    Spacer(Modifier.height(Dimens.sm))

                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.lg)) {
                        ProfileActionTile(
                            icon = Icons.Filled.ContentCopy,
                            iconColor = Accent,
                            title = "Copy User ID",
                            subtitle = user?.id ?: "",
                            onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(user?.id ?: "")) }
                        )
                        Spacer(Modifier.height(1.dp))
                        ProfileActionTile(
                            icon = Icons.Filled.ContentCopy,
                            iconColor = Warning,
                            title = "Copy Username",
                            subtitle = username,
                            onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(username)) }
                        )
                        Spacer(Modifier.height(1.dp))
                        ProfileActionTile(
                            icon = Icons.Filled.OpenInBrowser,
                            iconColor = Success,
                            title = "Open in Browser",
                            subtitle = "discord.com/users/${user?.id ?: ""}",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com/users/${user?.id ?: ""}"))
                                context.startActivity(intent)
                            }
                        )
                    }

                    Spacer(Modifier.height(Dimens.md))

                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.lg).clickable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null,
                            onClick = { viewModel.signOut() }
                        ),
                        contentPadding = PaddingValues(Dimens.lg)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Error.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out",
                                    tint = Error, modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sign Out",
                                    color = Error, fontSize = 15.sp, fontWeight = FontWeight.W600
                                )
                                Text(
                                    text = "Disconnect from Discord",
                                    color = TextSecondary, fontSize = 12.sp
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                                tint = Error.copy(alpha = 0.5f), modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(Dimens.xxl))
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String, onCopy: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.W500
            )
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (label != "Locale" && label != "Verified" && label != "MFA Enabled") {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Accent.copy(alpha = 0.12f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null,
                        onClick = onCopy
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ContentCopy, contentDescription = "Copy $label",
                    tint = Accent, modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
            onClick = onClick
        ),
        contentPadding = PaddingValues(Dimens.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, contentDescription = title,
                    tint = iconColor, modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.W600
                )
                Text(
                    text = subtitle,
                    color = TextSecondary, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = TextTertiary, modifier = Modifier.size(20.dp)
            )
        }
    }
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


