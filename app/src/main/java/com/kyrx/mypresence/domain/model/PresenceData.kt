package com.kyrx.mypresence.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PresenceData(
    val name: String = "My Presence",
    val type: Int = 0,
    val details: String = "",
    val state: String = "",
    val largeImage: String = "",
    val largeText: String = "",
    val smallImage: String = "",
    val smallText: String = "",
    val startTimestamp: Long? = null,
    val endTimestamp: Long? = null,
    val status: String = "online",
    val enabled: Boolean = false
) {
    companion object {
        val DEFAULT = PresenceData(
            name = "My Presence",
            type = 0,
            details = "Customizing my Discord presence",
            state = "via My Presence",
            largeImage = "icon",
            largeText = "My Presence",
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
