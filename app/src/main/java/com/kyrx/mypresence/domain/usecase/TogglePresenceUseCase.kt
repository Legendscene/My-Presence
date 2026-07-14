package com.kyrx.mypresence.domain.usecase

import android.app.Application
import android.content.Intent
import com.kyrx.mypresence.service.PresenceService
import javax.inject.Inject

class TogglePresenceUseCase @Inject constructor(
    private val application: Application
) {
    fun start() {
        val intent = Intent(application, PresenceService::class.java).apply {
            action = PresenceService.ACTION_START
        }
        try { application.startForegroundService(intent) } catch (_: Exception) {}
    }

    fun stop() {
        val intent = Intent(application, PresenceService::class.java).apply {
            action = PresenceService.ACTION_STOP
        }
        try { application.startForegroundService(intent) } catch (_: Exception) {}
    }
}
