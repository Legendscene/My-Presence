package com.kyrx.mypresence.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyrx.mypresence.domain.repository.AuthState
import com.kyrx.mypresence.ui.animation.StaggeredReveal
import com.kyrx.mypresence.ui.components.AnimatedLogo
import com.kyrx.mypresence.ui.components.GlassCard
import com.kyrx.mypresence.ui.components.GradientText
import com.kyrx.mypresence.ui.icons.DiscordIcon
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Blurple
import com.kyrx.mypresence.ui.theme.BlurpleLight
import com.kyrx.mypresence.ui.theme.GlassAppearance
import com.kyrx.mypresence.ui.theme.Gold
import com.kyrx.mypresence.ui.theme.SurfaceBorder
import com.kyrx.mypresence.ui.theme.SurfaceCard
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextSecondary
import com.kyrx.mypresence.ui.theme.TextTertiary
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    val isOAuthInProgress by viewModel.isOAuthInProgress.collectAsState()
    val oauthError by viewModel.oauthError.collectAsState()

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                showSuccess = true
                delay(1200)
                onLoginSuccess()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            StaggeredReveal(0, delayMs = 100) {
                AnimatedLogo(size = 100.dp, showOrbits = true)
            }

            Spacer(modifier = Modifier.height(32.dp))

            StaggeredReveal(1, delayMs = 150) {
                GradientText(
                    text = "My Presence",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.W700
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            StaggeredReveal(2, delayMs = 200) {
                Text(
                    text = if (showSuccess) "Connected" else "Show your presence on Discord",
                    color = TextSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            StaggeredReveal(3, delayMs = 250) {
                GlassCard(
                    appearance = GlassAppearance(
                        baseColor = SurfaceCard, alpha = 0.5f,
                        borderColor = Gold.copy(alpha = 0.1f),
                        glowColor = Gold.copy(alpha = 0.04f),
                        cornerRadius = 16.dp
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = Gold, modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Column {
                            Text("Secure Authentication", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.W600)
                            Text("OAuth2 with PKCE — Encrypted & secure", color = TextTertiary, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            StaggeredReveal(4, delayMs = 300) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(brush = Brush.horizontalGradient(listOf(Blurple, BlurpleLight)))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.startOAuth() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isOAuthInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = TextPrimary, strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            DiscordIcon(modifier = Modifier.size(20.dp), tint = TextPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Continue with Discord",
                                color = TextPrimary, fontSize = 16.sp,
                                fontWeight = FontWeight.W600, letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            if (oauthError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = oauthError!!,
                    color = Color(0xFFFF6B6B),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            } else if (authState is AuthState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (authState as AuthState.Error).message,
                    color = Color(0xFFFF6B6B),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }

            if (isOAuthInProgress) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.cancelOAuth() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Back from browser? Tap to cancel",
                        color = BlurpleLight, fontSize = 13.sp, fontWeight = FontWeight.W500
                    )
                }
            }
        }
    }
}
