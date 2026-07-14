package com.kyrx.mypresence.domain.usecase

import com.kyrx.mypresence.core.utils.Constants
import com.kyrx.mypresence.domain.model.AppInfo
import com.kyrx.mypresence.domain.model.PresenceData
import com.kyrx.mypresence.domain.repository.AssetRepository
import com.kyrx.mypresence.domain.repository.GatewayRepository
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdatePresenceUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val gatewayRepository: GatewayRepository,
    private val assetRepository: AssetRepository
) {
    private var activePresenceKey: String? = null
    private var activeStartedAtMs: Long? = null

    suspend operator fun invoke(detectedApp: AppInfo?): Boolean {
        val appConfigs = preferencesRepository.appPresenceConfigs.first()
        val customPresence = preferencesRepository.customPresence.first()

        val name: String
        val details: String
        val state: String
        val activityType: Int
        val startTimestamp: Long?
        val endTimestamp: Long?
        val status: String
        val largeImage: String
        val largeText: String
        val largeUrl: String
        val smallImage: String
        val smallText: String
        val button1Label: String
        val button1Url: String
        val button2Label: String
        val button2Url: String
        val partySize: Int?
        val partyMax: Int?
        val platform: String
        val streamUrl: String
        val presenceKey: String
        var needsAssetRetry = false

        if (detectedApp != null) {
            val cfg = appConfigs.find { it.packageName == detectedApp.packageName }
            if (cfg != null && (cfg.name.isNotBlank() || cfg.details.isNotBlank() || cfg.state.isNotBlank())) {
                name = cfg.name.ifBlank { detectedApp.appName }
                details = cfg.details.ifBlank { defaultDetailsFor(detectedApp) }
                state = cleanAttributionState(cfg.state).ifBlank { defaultStateFor(detectedApp) }
                activityType = if (cfg.activityType >= 0) cfg.activityType else defaultActivityTypeFor(detectedApp.packageName)
            } else {
                name = detectedApp.appName
                details = defaultDetailsFor(detectedApp)
                state = defaultStateFor(detectedApp)
                activityType = defaultActivityTypeFor(detectedApp.packageName)
            }
            presenceKey = "app:${detectedApp.packageName}:$name:$details:$state:$activityType"
            startTimestamp = stableStartFor(presenceKey)
            endTimestamp = null
            status = "online"

            val userToken = preferencesRepository.userToken.first()
            val mpKey = if (userToken.isNotBlank()) {
                resolveAppIconWithRetry(detectedApp, userToken)
            } else {
                null
            }
            needsAssetRetry = mpKey.isNullOrBlank()
            largeImage = if (!mpKey.isNullOrBlank()) mpKey else "icon"
            largeText = if (!mpKey.isNullOrBlank()) "My Presence app" else detectedApp.appName
            largeUrl = if (!mpKey.isNullOrBlank()) Constants.GITHUB_REPO_URL else ""
            smallImage = ""
            smallText = ""

            button1Label = ""
            button1Url = ""
            button2Label = ""
            button2Url = ""
            partySize = null
            partyMax = null
            platform = "android"
            streamUrl = ""
        } else {
            name = customPresence.name.ifBlank { "My Presence" }
            details = customPresence.details.ifBlank { "Using My Presence" }
            state = customPresence.state
            activityType = customPresence.type
            presenceKey = "custom:$name:$details:$state:$activityType:${customPresence.largeImage}:${customPresence.smallImage}:${customPresence.button1Label}:${customPresence.button2Label}"
            startTimestamp = customPresence.startTimestamp ?: stableStartFor(presenceKey)
            endTimestamp = customPresence.endTimestamp
            status = customPresence.status.ifBlank { "online" }
            largeImage = customPresence.largeImage
            largeText = customPresence.largeText
            largeUrl = customPresence.largeUrl.ifBlank { Constants.GITHUB_REPO_URL.takeIf { customPresence.largeImage.isNotBlank() }.orEmpty() }
            smallImage = customPresence.smallImage
            smallText = customPresence.smallText
            button1Label = customPresence.button1Label
            button1Url = customPresence.button1Url
            button2Label = customPresence.button2Label
            button2Url = customPresence.button2Url
            partySize = customPresence.partySize
            partyMax = customPresence.partyMax
            platform = customPresence.platform.ifBlank { "android" }
            streamUrl = customPresence.streamUrl
        }

        val presence = PresenceData(
            name = name,
            details = details,
            state = state,
            type = activityType,
            enabled = true,
            status = status,
            largeImage = largeImage,
            largeText = largeText,
            largeUrl = largeUrl,
            smallImage = smallImage,
            smallText = smallText,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            button1Label = button1Label,
            button1Url = button1Url,
            button2Label = button2Label,
            button2Url = button2Url,
            partySize = partySize,
            partyMax = partyMax,
            platform = platform,
            streamUrl = streamUrl
        )
        gatewayRepository.updatePresence(presence)
        return !needsAssetRetry
    }

    private fun stableStartFor(key: String): Long {
        val now = System.currentTimeMillis()
        if (activePresenceKey != key || activeStartedAtMs == null) {
            activePresenceKey = key
            activeStartedAtMs = now
        }
        return activeStartedAtMs ?: now
    }

    private suspend fun resolveAppIconWithRetry(app: AppInfo, userToken: String): String? {
        repeat(3) { attempt ->
            val resolved = assetRepository.resolveAppIcon(
                packageName = app.packageName,
                appName = app.appName,
                userToken = userToken,
                applicationId = Constants.RPC_APPLICATION_ID
            )
            if (!resolved.isNullOrBlank()) return resolved
            delay(350L * (attempt + 1))
        }
        return null
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
            else -> ""
        }
    }

    private fun cleanAttributionState(value: String): String {
        val normalized = value.trim().lowercase()
        return if (normalized == "via my presence" || normalized == "via my presence app") "" else value
    }
}
