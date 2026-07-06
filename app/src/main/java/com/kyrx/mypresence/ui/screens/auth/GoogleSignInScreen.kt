package com.kyrx.mypresence.ui.screens.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.browser.customtabs.CustomTabsIntent
import com.kyrx.mypresence.core.utils.Constants
import com.kyrx.mypresence.ui.animations.AnimatedMeshGradient
import com.kyrx.mypresence.ui.animations.ParticleAnimation
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Primary
import com.kyrx.mypresence.ui.theme.Secondary
import com.kyrx.mypresence.ui.theme.Surface
import com.kyrx.mypresence.ui.theme.SurfaceBorder
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextSecondary
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Discord brand colors
private val DiscordBlurple = Color(0xFF5865F2)

@Composable
fun LoginScreen(
    onDiscordSignIn: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onSignInSuccess: () -> Unit,
    onSignInError: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(1500)
            onSignInSuccess()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Background)
    ) {
        AnimatedMeshGradient(
            modifier = Modifier.fillMaxSize().blur(80.dp)
        )
        ParticleAnimation(
            modifier = Modifier.fillMaxSize().alpha(0.3f)
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Logo
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(spring(dampingRatio = Spring.DampingRatioLowBouncy)) +
                        slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy))
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (showSuccess) listOf(Primary, Primary) else listOf(Primary, Secondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (showSuccess) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) +
                        slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
            ) {
                Text(
                    text = if (showSuccess) "Welcome!" else "Welcome to My Presence",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) +
                        slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
            ) {
                Text(
                    text = if (showSuccess) "Signed in successfully" else "Sign in to manage your Discord Rich Presence",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (showSuccess) Primary else TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Security badge
            AnimatedVisibility(
                visible = visible && !showSuccess,
                enter = fadeIn(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) +
                        slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your data is secure and encrypted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main Discord Sign-In Button
            AnimatedVisibility(
                visible = visible && !showSuccess,
                enter = fadeIn(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) +
                        slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
            ) {
                Button(
                    onClick = {
                        isLoading = true
                        onDiscordSignIn()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Sign in with Discord",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Divider with "or"
            AnimatedVisibility(
                visible = visible && !showSuccess,
                enter = fadeIn(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) +
                        slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = SurfaceBorder
                    )
                    Text(
                        text = "or",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = SurfaceBorder
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // More Options Toggle
            AnimatedVisibility(
                visible = visible && !showSuccess,
                enter = fadeIn(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) +
                        slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                        .clickable { showMoreOptions = !showMoreOptions }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "More options",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (showMoreOptions) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // More Options Panel
            AnimatedVisibility(
                visible = showMoreOptions && !showSuccess,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                ) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    // Google Sign-In Button
                    OutlinedButton(
                        onClick = {
                            isLoading = true
                            onGoogleSignIn()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Sign in with Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Terms
            AnimatedVisibility(
                visible = visible && !showSuccess,
                enter = fadeIn(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) +
                        slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
            ) {
                Text(
                    text = "By continuing, you agree to our Terms of Service and Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Keep old name for navigation compatibility
@Composable
fun GoogleSignInScreen(
    onSignInSuccess: () -> Unit,
    onSignInError: (String) -> Unit
) {
    val context = LocalContext.current

    LoginScreen(
        onDiscordSignIn = {
            // Discord OAuth flow using Chrome Custom Tabs for better UX
            val clientId = Constants.CLIENT_ID
            val redirectUri = Constants.REDIRECT_URI
            val scope = "identify%20rpc"
            val discordAuthUrl = "https://discord.com/api/oauth2/authorize" +
                    "?client_id=$clientId" +
                    "&redirect_uri=$redirectUri" +
                    "&response_type=code" +
                    "&scope=$scope"

            // Open in Chrome Custom Tabs (better than regular browser)
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setToolbarColor(0xFF5865F2.toInt()) // Discord blurple
                .build()

            try {
                customTabsIntent.launchUrl(context, Uri.parse(discordAuthUrl))
            } catch (e: Exception) {
                // Fallback to regular browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(discordAuthUrl))
                context.startActivity(intent)
            }

            // Simulate success for now
            MainScope().launch {
                delay(2000)
                onSignInSuccess()
            }
        },
        onGoogleSignIn = {
            onSignInError("Google Sign-In coming soon with Firebase!")
        },
        onSignInSuccess = onSignInSuccess,
        onSignInError = onSignInError
    )
}
