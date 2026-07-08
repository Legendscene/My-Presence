package com.kyrx.mypresence.domain.usecase

import com.kyrx.mypresence.data.remote.DiscordGateway
import com.kyrx.mypresence.domain.model.AppInfo
import com.kyrx.mypresence.domain.model.PresenceData
import com.kyrx.mypresence.domain.repository.AuthRepository
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdatePresenceUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val discordGateway: DiscordGateway
) {
    suspend operator fun invoke(detectedApp: AppInfo?) {
        if (authRepository.isTokenExpired()) {
            val refreshed = authRepository.refreshAccessToken()
            if (!refreshed) return
        }

        val appConfigs = preferencesRepository.appPresenceConfigs.first()
        val presenceName = preferencesRepository.presenceName.first()
        val presenceDetails = preferencesRepository.presenceDetails.first()
        val presenceState = preferencesRepository.presenceState.first()
        val presenceType = preferencesRepository.presenceType.first()

        val name: String
        val details: String
        val state: String
        val activityType: Int

        if (detectedApp != null) {
            val cfg = appConfigs.find { it.packageName == detectedApp.packageName }
            if (cfg != null && (cfg.name.isNotBlank() || cfg.details.isNotBlank() || cfg.state.isNotBlank())) {
                name = cfg.name.ifBlank { detectedApp.appName }
                details = cfg.details.ifBlank { defaultDetailsFor(detectedApp) }
                state = cfg.state.ifBlank { defaultStateFor(detectedApp) }
                activityType = if (cfg.activityType >= 0) cfg.activityType else defaultActivityTypeFor(detectedApp.packageName)
            } else {
                name = detectedApp.appName
                details = defaultDetailsFor(detectedApp)
                state = defaultStateFor(detectedApp)
                activityType = defaultActivityTypeFor(detectedApp.packageName)
            }
        } else {
            name = presenceName.ifBlank { "My Presence" }
            details = presenceDetails.ifBlank { "Custom mobile presence" }
            state = presenceState.ifBlank { "Privacy-safe mode" }
            activityType = presenceType
        }

        val presence = PresenceData(
            name = name,
            details = details,
            state = state,
            type = activityType,
            enabled = true,
            status = "online",
            largeImage = "icon",
            largeText = "My Presence App",
            startTimestamp = System.currentTimeMillis()
        )
        discordGateway.updatePresence(presence)
    }

    private fun defaultActivityTypeFor(packageName: String): Int {
        val lower = packageName.lowercase()
        return when {
            lower.contains("spotify") || lower.contains("music") || lower.contains("soundcloud") -> 2
            lower.contains("youtube") || lower.contains("netflix") || lower.contains("instagram") || lower.contains("tiktok") -> 3
            else -> 0
        }
    }

    private fun defaultDetailsFor(app: AppInfo): String = when (defaultActivityTypeFor(app.packageName)) {
        2 -> "Listening on ${app.appName}"
        3 -> "Watching ${app.appName}"
        else -> "Using ${app.appName}"
    }

    private fun defaultStateFor(app: AppInfo): String {
        val lower = app.packageName.lowercase()
        return when {
            lower.contains("instagram") -> "Browsing feed"
            lower.contains("whatsapp") || lower.contains("telegram") || lower.contains("discord") -> "Messages are private"
            else -> "via My Presence"
        }
    }
}
