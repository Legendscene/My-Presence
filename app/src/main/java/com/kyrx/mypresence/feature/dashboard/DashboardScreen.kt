package com.kyrx.mypresence.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyrx.mypresence.domain.repository.AuthState
import com.kyrx.mypresence.feature.dashboard.components.*
import com.kyrx.mypresence.feature.dashboard.model.QuickAction

@Composable
fun DashboardScreen(
    vm: DashboardViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToExperimental: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val authState by vm.authState.collectAsStateWithLifecycle()
    val gatewayState by vm.gatewayState.collectAsStateWithLifecycle()
    val presenceEnabled by vm.presenceEnabled.collectAsStateWithLifecycle()
    val detectedApp by vm.detectedApp.collectAsStateWithLifecycle()
    val enabledApps by vm.enabledApps.collectAsStateWithLifecycle()
    val installedApps by vm.installedApps.collectAsStateWithLifecycle()
    val keepOnline by vm.keepOnline24_7.collectAsStateWithLifecycle()
    val appConfigs by vm.appPresenceConfigs.collectAsStateWithLifecycle()

    var showLogoutConfirm by remember { mutableStateOf(false) }

    val user = (authState as? AuthState.Authenticated)?.user

    val quickActions = listOf(
        QuickAction("settings", "Settings", "Customize your presence", Icons.Filled.Settings) { onNavigateToSettings() },
        QuickAction("profile", "Profile", "View your Discord profile", Icons.Filled.Person) { onNavigateToProfile() },
        QuickAction("experimental", "Experimental", "Try new features", Icons.Filled.Build) { onNavigateToExperimental() },
        QuickAction("diagnostics", "Diagnostics", "Connection diagnostics", Icons.Filled.BugReport) { onNavigateToDiagnostics() }
    )

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to disconnect your Discord account?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    vm.logout()
                    onLogout()
                }) { Text("Logout") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                ProfileHeader(
                    user = user,
                    gatewayState = gatewayState,
                    onSettings = onNavigateToSettings,
                    onLogout = { showLogoutConfirm = true }
                )
            }

            item { StatusCards(gatewayState = gatewayState) }

            item { PresenceToggle(enabled = presenceEnabled, onToggle = { vm.togglePresence(it) }) }

            if (presenceEnabled) {
                item { CurrentActivity(presenceEnabled = presenceEnabled, detectedApp = detectedApp) }
            }

            item {
                AppTracker(
                    installedApps = installedApps,
                    enabledApps = enabledApps,
                    detectedApp = detectedApp,
                    appConfigs = appConfigs,
                    onToggleApp = { pkg, enabled -> vm.toggleApp(pkg, enabled) },
                    onEditApp = { vm.saveAppPresenceConfig(vm.previewPresenceFor(it).toConfig(it.packageName)) },
                    hasUsageStatsPermission = vm.hasUsageStatsPermission(),
                    requestUsageAccess = { context.startActivity(vm.requestUsageAccessIntent()) },
                    previewPresence = { vm.previewPresenceFor(it).toPreviewString() }
                )
            }

            item { QuickActionsSection(actions = quickActions) }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
