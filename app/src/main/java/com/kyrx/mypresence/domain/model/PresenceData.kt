package com.kyrx.mypresence.domain.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class PresenceData(
    val name: String = "My Presence",
    val type: Int = 0,
    val details: String = "",
    val state: String = "",
    val largeImage: String = "",
    val largeText: String = "",
    val largeUrl: String = "",
    val smallImage: String = "",
    val smallText: String = "",
    val startTimestamp: Long? = null,
    val endTimestamp: Long? = null,
    val status: String = "online",
    val enabled: Boolean = false,
    val button1Label: String = "",
    val button1Url: String = "",
    val button2Label: String = "",
    val button2Url: String = "",
    val partySize: Int? = null,
    val partyMax: Int? = null,
    val platform: String = "android",
    val streamUrl: String = ""
) {
    companion object {
        val DEFAULT = PresenceData(
            name = "My Presence",
            type = 0,
            details = "Customizing my Discord presence",
            state = "",
            largeImage = "icon",
            largeText = "My Presence",
            largeUrl = "https://github.com/Legendscene/mypresence",
            status = "online"
        )
    }

    fun toPreviewString(): String = if (details.isNotBlank()) details else name

    fun toConfig(packageName: String): AppPresenceConfig = AppPresenceConfig(
        packageName = packageName,
        name = name,
        details = details,
        state = state,
        activityType = type
    )
}
