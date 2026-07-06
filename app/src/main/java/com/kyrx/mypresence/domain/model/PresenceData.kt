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
}

sealed class GatewayConnectionState {
    data object Disconnected : GatewayConnectionState()
    data object Connecting : GatewayConnectionState()
    data class Connected(val sessionId: String, val sequence: Int?) : GatewayConnectionState()
    data class Error(val message: String) : GatewayConnectionState()
}
