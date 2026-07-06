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
import com.kyrx.mypresence.core.utils.Constants
import com.kyrx.mypresence.data.remote.DiscordGateway
import com.kyrx.mypresence.domain.model.GatewayConnectionState
import com.kyrx.mypresence.domain.model.PresenceData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PresenceService : Service() {

    @Inject lateinit var discordGateway: DiscordGateway

    companion object {
        const val CHANNEL_ID = "presence_service"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.kyrx.mypresence.START"
        const val ACTION_STOP = "com.kyrx.mypresence.STOP"
        const val ACTION_UPDATE = "com.kyrx.mypresence.UPDATE"
        const val EXTRA_PRESENCE = "extra_presence"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var presenceJob: Job? = null

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
            ACTION_START -> startPresence()
            ACTION_STOP -> stopPresence()
            ACTION_UPDATE -> {
                val name = intent.getStringExtra("name") ?: "My Presence"
                val details = intent.getStringExtra("details") ?: ""
                val state = intent.getStringExtra("state") ?: ""
                val type = intent.getIntExtra("type", 0)
                val status = intent.getStringExtra("status") ?: "online"
                val enabled = intent.getBooleanExtra("enabled", true)
                updatePresenceData(
                    PresenceData(
                        name = name, details = details, state = state,
                        type = type, status = status, enabled = enabled
                    )
                )
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun startPresence() {
        val notification = buildNotification("Connecting to Discord...")
        startForeground(NOTIFICATION_ID, notification)
        serviceScope.launch {
            discordGateway.connect(Constants.DISCORD_TOKEN)
        }
    }

    private fun updatePresenceData(presence: PresenceData) {
        if (presence.enabled) {
            serviceScope.launch {
                discordGateway.updatePresence(presence)
            }
        }
    }

    private fun observeGatewayState() {
        serviceScope.launch {
            discordGateway.state.collect { state ->
                _connectionState.value = state
                val notificationText = when (state) {
                    is GatewayConnectionState.Connected -> "Presence active"
                    is GatewayConnectionState.Connecting -> "Connecting..."
                    is GatewayConnectionState.Disconnected -> "Disconnected"
                    is GatewayConnectionState.Error -> "Error: ${state.message}"
                }
                updateNotification(notificationText)
            }
        }
    }

    private fun stopPresence() {
        presenceJob?.cancel()
        discordGateway.disconnect()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        stopPresence()
        serviceScope.cancel()
        super.onDestroy()
    }
}
