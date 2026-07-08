package com.kyrx.mypresence.domain.usecase

import android.app.Application
import android.content.Intent
import com.kyrx.mypresence.data.remote.DiscordGateway
import com.kyrx.mypresence.domain.repository.AuthRepository
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import com.kyrx.mypresence.service.PresenceService
import javax.inject.Inject

class TogglePresenceUseCase @Inject constructor(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val discordGateway: DiscordGateway,
    private val updatePresence: UpdatePresenceUseCase,
    private val detectForegroundApp: DetectForegroundAppUseCase
) {
    suspend fun start(): Boolean {
        if (authRepository.isTokenExpired()) {
            val refreshed = authRepository.refreshAccessToken()
            if (!refreshed) return false
        }
        val token = authRepository.getAccessToken() ?: return false
        try { discordGateway.connect(token) } catch (_: Exception) { return false }
        try { discordGateway.awaitConnected() } catch (_: Exception) { return false }

        val detectedApp = detectForegroundApp.getForegroundApp()
        updatePresence(detectedApp)

        val intent = Intent(application, PresenceService::class.java).apply {
            action = PresenceService.ACTION_START
        }
        try { application.startForegroundService(intent) } catch (_: Exception) {}
        return true
    }

    fun stop() {
        discordGateway.disconnect()
        val intent = Intent(application, PresenceService::class.java).apply {
            action = PresenceService.ACTION_STOP
        }
        application.startService(intent)
    }
}
