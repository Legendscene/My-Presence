package com.kyrx.mypresence.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyrx.mypresence.ui.components.PremiumCard
import com.kyrx.mypresence.ui.components.SettingsTile
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Primary
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    var headerVisible by remember { mutableStateOf(false) }
    var appearanceVisible by remember { mutableStateOf(false) }
    var notificationsVisible by remember { mutableStateOf(false) }
    var presenceVisible by remember { mutableStateOf(false) }
    var aboutVisible by remember { mutableStateOf(false) }
    var darkMode by remember { mutableStateOf(true) }
    var notifications by remember { mutableStateOf(true) }
    var autoStart by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        headerVisible = true
        kotlinx.coroutines.delay(100)
        appearanceVisible = true
        kotlinx.coroutines.delay(100)
        notificationsVisible = true
        kotlinx.coroutines.delay(100)
        presenceVisible = true
        kotlinx.coroutines.delay(100)
        aboutVisible = true
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = TextPrimary,
                navigationIconContentColor = TextPrimary
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Appearance Section
            AnimatedVisibility(
                visible = appearanceVisible,
                enter = fadeIn(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                ) + slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                )
            ) {
                SectionHeader(title = "Appearance")
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = appearanceVisible,
                enter = fadeIn(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                ) + slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                )
            ) {
                PremiumCard {
                    SettingsTile(
                        icon = Icons.Filled.DarkMode,
                        title = "Dark Mode",
                        subtitle = if (darkMode) "On" else "Off",
                        onClick = { darkMode = !darkMode }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notifications Section
            AnimatedVisibility(
                visible = notificationsVisible,
                enter = fadeIn(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                ) + slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                )
            ) {
                SectionHeader(title = "Notifications")
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = notificationsVisible,
                enter = fadeIn(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                ) + slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                )
            ) {
                PremiumCard {
                    SettingsTile(
                        icon = Icons.Filled.Notifications,
                        title = "Push Notifications",
                        subtitle = if (notifications) "Enabled" else "Disabled",
                        onClick = { notifications = !notifications }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Presence Section
            AnimatedVisibility(
                visible = presenceVisible,
                enter = fadeIn(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                ) + slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                )
            ) {
                SectionHeader(title = "Presence")
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = presenceVisible,
                enter = fadeIn(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                ) + slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                )
            ) {
                PremiumCard {
                    SettingsTile(
                        icon = Icons.Filled.Bolt,
                        title = "Auto-start on Boot",
                        subtitle = if (autoStart) "Enabled" else "Disabled",
                        onClick = { autoStart = !autoStart }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About Section
            AnimatedVisibility(
                visible = aboutVisible,
                enter = fadeIn(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                ) + slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                )
            ) {
                SectionHeader(title = "About")
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = aboutVisible,
                enter = fadeIn(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                ) + slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                )
            ) {
                Column {
                    PremiumCard {
                        SettingsTile(
                            icon = Icons.Filled.Info,
                            title = "Version",
                            subtitle = "1.0.0",
                            onClick = {}
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumCard {
                        SettingsTile(
                            icon = Icons.Filled.Star,
                            title = "Rate App",
                            onClick = {}
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumCard {
                        SettingsTile(
                            icon = Icons.Filled.Favorite,
                            title = "Support",
                            onClick = {}
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumCard {
                        SettingsTile(
                            icon = Icons.Filled.PrivacyTip,
                            title = "Privacy Policy",
                            onClick = {}
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumCard {
                        SettingsTile(
                            icon = Icons.Filled.Code,
                            title = "Open Source Licenses",
                            onClick = {}
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = Primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}
