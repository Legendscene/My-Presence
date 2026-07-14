package com.kyrx.mypresence.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kyrx.mypresence.MainActivity
import com.kyrx.mypresence.R
import com.kyrx.mypresence.core.analytics.CrashReporter
import com.kyrx.mypresence.core.gateway.GatewayConnectionState
import com.kyrx.mypresence.core.utils.Constants
import com.kyrx.mypresence.domain.model.AppInfo
import com.kyrx.mypresence.domain.repository.AppRepository
import com.kyrx.mypresence.domain.repository.AssetRepository
import com.kyrx.mypresence.domain.repository.GatewayRepository
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import com.kyrx.mypresence.domain.usecase.UpdatePresenceUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PresenceService : Service() {

    @Inject lateinit var gatewayRepository: GatewayRepository
    @Inject lateinit var appRepository: AppRepository
    @Inject lateinit var assetRepository: AssetRepository
    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var updatePresence: UpdatePresenceUseCase
    @Inject lateinit var crashReporter: CrashReporter

    companion object {
        const val CHANNEL_ID = "presence_service"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.kyrx.mypresence.START"
        const val ACTION_STOP = "com.kyrx.mypresence.STOP"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null
    private var resendJob: Job? = null
    private var assetWarmupJob: Job? = null
    private var assetRetryJob: Job? = null
    private var lastPresenceKey: String? = null
    private var lastPresenceSentMs: Long = 0L

    private val _connectionState = MutableStateFlow<GatewayConnectionState>(GatewayConnectionState.Disconnected)
    val connectionState: StateFlow<GatewayConnectionState> = _connectionState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observeGatewayState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> stopPresence()
            else -> startPresence()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun startPresence() {
        crashReporter.log("PresenceService startPresence")
        val notification = buildNotification("My Presence is active")
        try { startForeground(NOTIFICATION_ID, notification) } catch (_: Exception) {
            crashReporter.logNonFatal(Exception("startForeground failed"))
        }
        serviceScope.launch {
            preferencesRepository.setPresenceEnabled(true)
            if (gatewayRepository.state.value !is GatewayConnectionState.Connected &&
                gatewayRepository.state.value !is GatewayConnectionState.Connecting &&
                gatewayRepository.state.value !is GatewayConnectionState.Authenticating
            ) {
                gatewayRepository.connect()
            }
        }
        startPresenceSync()
        startAssetWarmup()
    }

    private fun observeGatewayState() {
        serviceScope.launch {
            gatewayRepository.state.collect { state ->
                _connectionState.value = state
                val notificationText = when (state) {
                    is GatewayConnectionState.Connected -> "Presence active"
                    is GatewayConnectionState.Connecting -> "Connecting..."
                    is GatewayConnectionState.Authenticating -> "Authenticating..."
                    is GatewayConnectionState.Resuming -> "Resuming session..."
                    is GatewayConnectionState.Reconnecting -> "Reconnecting..."
                    is GatewayConnectionState.Disconnected -> "Disconnected"
                    is GatewayConnectionState.HeartbeatLost -> "Connection unstable"
                    is GatewayConnectionState.RateLimited -> "Rate limited"
                    is GatewayConnectionState.InvalidSession -> "Session invalid"
                    is GatewayConnectionState.Unauthorized -> "Auth required"
                    is GatewayConnectionState.FatalError -> "Connection error"
                }
                updateNotification(notificationText)
                if (state is GatewayConnectionState.Connected) {
                    delay(1000)
                    sendCurrentPresence(appRepository.foregroundApp.value, "gateway-connected")
                }
            }
        }
    }

    private fun stopPresence() {
        syncJob?.cancel()
        resendJob?.cancel()
        assetWarmupJob?.cancel()
        assetRetryJob?.cancel()
        syncJob = null
        resendJob = null
        assetWarmupJob = null
        assetRetryJob = null
        lastPresenceKey = null
        gatewayRepository.disconnect()
        serviceScope.launch { preferencesRepository.setPresenceEnabled(false) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startPresenceSync() {
        if (syncJob?.isActive == true) return
        syncJob = serviceScope.launch {
            appRepository.foregroundApp.collect { app ->
                sendCurrentPresence(app, "foreground-change")
            }
        }
        resendJob?.cancel()
        resendJob = serviceScope.launch {
            while (isActive) {
                delay(30_000)
                sendCurrentPresence(appRepository.foregroundApp.value, "periodic-resend", force = true)
            }
        }
    }

    private fun startAssetWarmup() {
        if (assetWarmupJob?.isActive == true) return
        assetWarmupJob = serviceScope.launch {
            val token = preferencesRepository.userToken.first()
            if (token.isBlank() || Constants.RPC_APPLICATION_ID.isBlank()) return@launch

            var attempts = 0
            while (appRepository.installedApps.value.isEmpty() && attempts < 20 && isActive) {
                delay(250)
                attempts++
            }

            val installed = appRepository.installedApps.value
            val priorityPackages = listOf(
                "com.whatsapp",
                "com.google.android.apps.nbu.paisa.user",
                "com.instagram.android",
                "com.spotify.music",
                "com.google.android.youtube",
                "org.telegram.messenger",
                "com.snapchat.android",
                "com.twitter.android",
                "com.discord",
                "com.google.android.gm"
            )
            val priority = priorityPackages.mapNotNull { pkg ->
                installed.firstOrNull { it.packageName == pkg }
            }
            val rest = installed
                .filterNot { app -> priority.any { it.packageName == app.packageName } }
                .take(40)

            (priority + rest).distinctBy { it.packageName }.forEachIndexed { index, app ->
                if (!isActive) return@forEachIndexed
                crashReporter.log("Asset warmup ${index + 1}: ${app.packageName}")
                assetRepository.resolveAppIcon(
                    packageName = app.packageName,
                    appName = app.appName,
                    userToken = token,
                    applicationId = Constants.RPC_APPLICATION_ID
                )
                delay(350)
            }
        }
    }

    private suspend fun sendCurrentPresence(foreground: AppInfo?, source: String, force: Boolean = false) {
        val enabled = preferencesRepository.presenceEnabled.first()
        if (!enabled || gatewayRepository.state.value !is GatewayConnectionState.Connected) return

        val appForPresence = foreground
        if (appForPresence == null) {
            if (lastPresenceKey != null || force) {
                crashReporter.log("PresenceService clear source=$source")
                gatewayRepository.clearPresence()
                lastPresenceKey = null
                lastPresenceSentMs = System.currentTimeMillis()
                assetRetryJob?.cancel()
            }
            return
        }

        val key = "app:${appForPresence.packageName}"
        val now = System.currentTimeMillis()
        if (!force && key == lastPresenceKey && now - lastPresenceSentMs < 2_500L) return

        crashReporter.log("PresenceService send source=$source key=$key")
        val sent = updatePresence(appForPresence)
        if (sent) {
            lastPresenceKey = key
            lastPresenceSentMs = now
            assetRetryJob?.cancel()
        } else {
            scheduleAssetRetry(appForPresence)
        }
    }

    private fun scheduleAssetRetry(app: AppInfo) {
        assetRetryJob?.cancel()
        assetRetryJob = serviceScope.launch {
            delay(3_000)
            if (appRepository.foregroundApp.value?.packageName == app.packageName) {
                sendCurrentPresence(app, "asset-retry", force = true)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Presence Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps your Discord presence active"
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, PresenceService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("My Presence")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        syncJob?.cancel()
        resendJob?.cancel()
        assetWarmupJob?.cancel()
        assetRetryJob?.cancel()
        gatewayRepository.disconnect()
        serviceScope.cancel()
        super.onDestroy()
    }
}
