package com.kyrx.mypresence.feature.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyrx.mypresence.ui.theme.Accent
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Dimens
import com.kyrx.mypresence.ui.theme.Surface
import com.kyrx.mypresence.ui.theme.SurfaceBorder
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextSecondary
import com.kyrx.mypresence.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit = {}
) {
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val autoStartEnabled by viewModel.autoStartEnabled.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val analyticsEnabled by viewModel.analyticsEnabled.collectAsState()

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.lg)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsGroupHeader("General")

            SettingsToggleItem(
                icon = Icons.Filled.Notifications,
                label = "Notifications",
                subtitle = "Show notifications when presence updates",
                checked = notificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
            )

            SettingsToggleItem(
                icon = Icons.Filled.PlayArrow,
                label = "Auto-start",
                subtitle = "Start presence on app launch",
                checked = autoStartEnabled,
                onCheckedChange = { viewModel.setAutoStartEnabled(it) }
            )

            SettingsToggleItem(
                icon = Icons.Filled.DarkMode,
                label = "Dark theme",
                subtitle = "Always use dark mode",
                checked = isDarkMode,
                onCheckedChange = { viewModel.setIsDarkMode(it) }
            )

            Spacer(Modifier.height(Dimens.xl))

            SettingsGroupHeader("Privacy")

            SettingsToggleItem(
                icon = Icons.Filled.BugReport,
                label = "Analytics",
                subtitle = "Send anonymous usage data to improve the app",
                checked = analyticsEnabled,
                onCheckedChange = { viewModel.setAnalyticsEnabled(it) }
            )

            Spacer(Modifier.height(Dimens.xl))

            SettingsGroupHeader("Other")

            SettingsNavItem(
                icon = Icons.Filled.Info,
                label = "About",
                subtitle = "Version and credits",
                onClick = onNavigateToAbout
            )

            Spacer(Modifier.height(Dimens.xxxl))
        }
    }
}

@Composable
private fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = Accent,
        fontWeight = FontWeight.W600,
        modifier = Modifier.padding(bottom = Dimens.sm, top = Dimens.xs)
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(Dimens.iconMedium)
        )
        Spacer(Modifier.width(Dimens.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Accent,
                checkedTrackColor = Accent.copy(alpha = 0.3f),
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = SurfaceBorder
            )
        )
    }
}

@Composable
private fun SettingsNavItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(Dimens.iconMedium)
        )
        Spacer(Modifier.width(Dimens.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(Dimens.iconMedium)
        )
    }
}
