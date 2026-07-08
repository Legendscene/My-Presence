package com.kyrx.mypresence.feature.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.kyrx.mypresence.core.utils.Constants
import com.kyrx.mypresence.ui.animation.StaggeredReveal
import com.kyrx.mypresence.ui.components.GlassCard
import com.kyrx.mypresence.ui.components.GradientText
import com.kyrx.mypresence.ui.components.PremiumSwitch
import com.kyrx.mypresence.ui.icons.GoogleIcon
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
import com.kyrx.mypresence.feature.settings.SettingsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val autoStartEnabled by viewModel.autoStartEnabled.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    var googleUser by remember { mutableStateOf(firebaseAuth.currentUser) }
    var googleLoading by remember { mutableStateOf(false) }
    var googleError by remember { mutableStateOf<String?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                scope.launch {
                    googleLoading = true
                    try {
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        firebaseAuth.signInWithCredential(credential).await()
                        googleUser = firebaseAuth.currentUser
                        googleError = null
                    } catch (_: Exception) {
                        googleError = "Google connect failed"
                    } finally {
                        googleLoading = false
                    }
                }
            } catch (_: ApiException) {
                googleError = "Google sign-in cancelled"
                googleLoading = false
            }
        } else {
            googleLoading = false
        }
    }

    fun startGoogleConnect() {
        googleError = null
        googleLoading = true
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(Constants.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        val client: GoogleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── Top Bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            GradientText(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.W700
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 80.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Appearance ──
            StaggeredReveal(0) {
                SectionHeader("Appearance")
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(
                    appearance = GlassAppearance(
                        baseColor = SurfaceCard,
                        alpha = 0.6f,
                        borderColor = SurfaceBorder.copy(alpha = 0.2f),
                        cornerRadius = 18.dp
                    )
                ) {
                    SettingsToggleTile(
                        icon = Icons.Filled.DarkMode,
                        iconColor = Gold,
                        title = "Dark Mode",
                        subtitle = if (isDarkMode) "Dark theme enabled" else "Light theme enabled",
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.setIsDarkMode(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Notifications ──
            StaggeredReveal(1) {
                SectionHeader("Accounts")
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(
                    appearance = GlassAppearance(
                        baseColor = SurfaceCard,
                        alpha = 0.6f,
                        borderColor = SurfaceBorder.copy(alpha = 0.2f),
                        cornerRadius = 18.dp
                    )
                ) {
                    GoogleAccountTile(
                        email = googleUser?.email,
                        loading = googleLoading,
                        error = googleError,
                        onConnect = { startGoogleConnect() },
                        onDisconnect = {
                            firebaseAuth.signOut()
                            googleUser = null
                            googleError = null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredReveal(2) {
                SectionHeader("Notifications")
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(
                    appearance = GlassAppearance(
                        baseColor = SurfaceCard,
                        alpha = 0.6f,
                        borderColor = SurfaceBorder.copy(alpha = 0.2f),
                        cornerRadius = 18.dp
                    )
                ) {
                    SettingsToggleTile(
                        icon = Icons.Filled.Notifications,
                        iconColor = Blurple,
                        title = "Push Notifications",
                        subtitle = "Receive connection alerts",
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Presence ──
            StaggeredReveal(2) {
                SectionHeader("Presence")
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(
                    appearance = GlassAppearance(
                        baseColor = SurfaceCard,
                        alpha = 0.6f,
                        borderColor = SurfaceBorder.copy(alpha = 0.2f),
                        cornerRadius = 18.dp
                    )
                ) {
                    SettingsToggleTile(
                        icon = Icons.Filled.PlayArrow,
                        iconColor = com.kyrx.mypresence.ui.theme.Success,
                        title = "Auto-start on Boot",
                        subtitle = "Connect automatically when device starts",
                        checked = autoStartEnabled,
                        onCheckedChange = { viewModel.setAutoStartEnabled(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── About ──
            StaggeredReveal(3) {
                SectionHeader("About")
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(
                    appearance = GlassAppearance(
                        baseColor = SurfaceCard,
                        alpha = 0.6f,
                        borderColor = SurfaceBorder.copy(alpha = 0.2f),
                        cornerRadius = 18.dp
                    )
                ) {
                    SettingsInfoTile(
                        icon = Icons.Filled.Info,
                        iconColor = GoldLight,
                        title = "Version",
                        subtitle = "1.0.0 — Production Build"
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = TextTertiary,
        fontSize = 12.sp,
        fontWeight = FontWeight.W600,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun GoogleAccountTile(
    email: String?,
    loading: Boolean,
    error: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (email == null) onConnect() else onDisconnect() }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center
        ) {
            GoogleIcon(modifier = Modifier.size(22.dp), tint = Color.White)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Google Account",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.W600
            )
            Text(
                text = when {
                    loading -> "Connecting..."
                    error != null -> error
                    email != null -> email
                    else -> "Tap to connect Google"
                },
                color = if (error != null) com.kyrx.mypresence.ui.theme.Error else TextSecondary,
                fontSize = 12.sp
            )
        }
        Text(
            text = if (email == null) "Connect" else "Disconnect",
            color = Gold,
            fontSize = 12.sp,
            fontWeight = FontWeight.W600
        )
    }
}

@Composable
private fun SettingsToggleTile(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
        PremiumSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun SettingsInfoTile(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
