package com.kyrx.mypresence.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.kyrx.mypresence.core.di.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                val prefs = context.dataStore.data.first()
                val autoStart = prefs[KEY_AUTO_START] ?: false
                if (autoStart) {
                    val serviceIntent = Intent(context, PresenceService::class.java).apply {
                        action = PresenceService.ACTION_START
                    }
                    try { context.startForegroundService(serviceIntent) } catch (_: Exception) {}
                }
            }
        }
    }

    private companion object {
        val KEY_AUTO_START = booleanPreferencesKey("auto_start_enabled")
    }
}
