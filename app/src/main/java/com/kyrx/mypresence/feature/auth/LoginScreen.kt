package com.kyrx.mypresence.feature.auth

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.kyrx.mypresence.BuildConfig
import com.kyrx.mypresence.domain.repository.AuthState
import com.kyrx.mypresence.ui.theme.Accent
import com.kyrx.mypresence.ui.theme.AccentMuted
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Dimens
import com.kyrx.mypresence.ui.theme.Error
import com.kyrx.mypresence.ui.theme.SurfaceBorder
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextSecondary
import com.kyrx.mypresence.ui.theme.TextTertiary

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val isLoginInProgress by authViewModel.isLoginInProgress.collectAsState()
    val loginError by authViewModel.loginError.collectAsState()
    val showMoreOptions by authViewModel.showMoreOptions.collectAsState()
    var showWebView by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java) as GoogleSignInAccount
            Log.i("GoogleSignIn", "Google sign-in successful: ${account.displayName} ${account.email}")
            if (account.idToken != null) {
                authViewModel.handleGoogleSignIn(account.idToken!!, account.displayName, account.email)
            } else {
                Log.e("GoogleSignIn", "Google sign-in failed: no ID token")
            }
        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "Google sign-in failed: ${e.statusCode} ${e.message}")
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "Google sign-in failed: ${e.message}")
        }
    }

    val fadeAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800),
        label = "fadeIn"
    )

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onLoginSuccess()
        }
    }

    if (showWebView) {
        BackHandler(onBack = { showWebView = false })
        DiscordLoginWebView(
            onLoginCompleted = { token ->
                showWebView = false
                authViewModel.loginWithToken(token)
            }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Dimens.xxl)
            .alpha(fadeAlpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(0.5f))

        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(com.kyrx.mypresence.R.drawable.ic_discord),
                contentDescription = "Discord",
                tint = Accent,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(Modifier.height(Dimens.xl))

        Text(
            text = "My Presence",
            style = MaterialTheme.typography.displaySmall,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(Dimens.sm))

        Text(
            text = "Show your activity on Discord",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.weight(0.25f))

        if (loginError != null) {
            Text(
                text = loginError!!,
                color = Error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = Dimens.md)
            )
        }

        Button(
            onClick = { showWebView = true },
            enabled = !isLoginInProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.buttonHeight),
            shape = RoundedCornerShape(Dimens.buttonCorner),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = TextPrimary
            )
        ) {
            Text(
                text = if (isLoginInProgress) "Opening Discord..." else "Log in with Discord",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.W600
            )
        }

        Spacer(Modifier.height(Dimens.sm))

        Text(
            text = "Login with your Discord credentials",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(Dimens.md))

        Row(
            modifier = Modifier
                .clickable { authViewModel.toggleMoreOptions() }
                .padding(vertical = Dimens.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "More options",
                style = MaterialTheme.typography.bodySmall,
                color = AccentMuted
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Expand",
                tint = AccentMuted,
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 2.dp)
            )
        }

        AnimatedVisibility(
            visible = showMoreOptions,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedButton(
                    onClick = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                            .requestEmail()
                            .build()
                        val googleClient = GoogleSignIn.getClient(context, gso)
                        googleClient.signOut()
                        googleSignInLauncher.launch(googleClient.signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(Dimens.buttonCorner),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(com.kyrx.mypresence.R.drawable.ic_google),
                        contentDescription = "Google",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Sign in with Google",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.W500
                    )
                }
            }
        }

        Spacer(Modifier.weight(0.2f))
    }
}
