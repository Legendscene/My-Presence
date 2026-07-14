package com.kyrx.mypresence.feature.onboarding

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import com.kyrx.mypresence.ui.theme.Accent
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Dimens
import com.kyrx.mypresence.ui.theme.Success
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PermissionItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isGranted: Boolean
)

@Composable
fun OnboardingScreen(
    preferencesRepository: PreferencesRepository,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var usageStatsGranted by remember { mutableStateOf(checkUsageStats(context)) }
    var batteryOptGranted by remember { mutableStateOf(checkBatteryOpt(context)) }
    var notificationGranted by remember { mutableStateOf(checkNotification(context)) }

    val allGranted = usageStatsGranted && batteryOptGranted && notificationGranted

    val usageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        usageStatsGranted = checkUsageStats(context)
    }

    val batteryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        batteryOptGranted = checkBatteryOpt(context)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.lg)
        ) {
            Text(
                text = "Get Started",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "My Presence needs a few permissions to run reliably",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(Dimens.md))

            PermissionRow(
                title = "Usage Access",
                description = "Detect which app you're currently using",
                icon = Icons.Filled.Visibility,
                isGranted = usageStatsGranted,
                onClick = {
                    usageLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )

            PermissionRow(
                title = "Ignore Battery Optimization",
                description = "Keep the connection alive in background",
                icon = Icons.Filled.Info,
                isGranted = batteryOptGranted,
                onClick = {
                    batteryLauncher.launch(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            )

            PermissionRow(
                title = "Notifications",
                description = "Required for persistent foreground service",
                icon = Icons.Filled.Notifications,
                isGranted = notificationGranted,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        notificationGranted = true
                    }
                }
            )

            PermissionRow(
                title = "Foreground Service",
                description = "Runs automatically when presence is active",
                icon = Icons.Filled.PlayArrow,
                isGranted = true,
                onClick = {}
            )

            PermissionRow(
                title = "Auto Start (OEM)",
                description = "Open system settings to enable (optional)",
                icon = Icons.Filled.Build,
                isGranted = true,
                onClick = {
                    openAutoStartSettings(context)
                }
            )

            Spacer(Modifier.height(Dimens.lg))

            Button(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        preferencesRepository.setOnboardingCompleted(true)
                    }
                    onComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(Dimens.md),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (allGranted) Accent else Accent.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = if (allGranted) "Start Using My Presence" else "Skip for now",
                    color = Color.White,
                    fontWeight = FontWeight.W600,
                    fontSize = 16.sp
                )
            }

            if (!allGranted) {
                Text(
                    text = "You can enable permissions later in Settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isGranted) Success.copy(alpha = 0.08f) else Color.Transparent,
                RoundedCornerShape(Dimens.sm)
            )
            .padding(Dimens.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted) Success else TextSecondary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(Dimens.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.W600,
                fontSize = 15.sp
            )
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        if (!isGranted) {
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Grant", fontSize = 12.sp)
            }
        } else {
            Text(
                text = "Granted",
                color = Success,
                fontWeight = FontWeight.W600,
                fontSize = 13.sp
            )
        }
    }
}

private fun checkUsageStats(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun checkBatteryOpt(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun checkNotification(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true
}

private fun checkAutoStart(context: Context): Boolean {
    return try {
        val pm = context.packageManager
        pm.getLaunchIntentForPackage("com.miui.securitycenter") != null
    } catch (_: Exception) { false }
}

private fun openAutoStartSettings(context: Context) {
    val intents = listOf(
        Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = android.net.Uri.parse("package:${context.packageName}")
        }
    )
    try {
        context.startActivity(intents[0])
    } catch (_: Exception) {}
}