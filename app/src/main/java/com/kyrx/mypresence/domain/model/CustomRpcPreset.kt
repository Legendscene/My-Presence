package com.kyrx.mypresence.domain.model

data class CustomRpcPreset(
    val id: String,
    val name: String,
    val details: String = "",
    val state: String = "",
    val largeImage: String = "",
    val largeText: String = "",
    val smallImage: String = "",
    val smallText: String = "",
    val button1Label: String = "",
    val button1Url: String = "",
    val button2Label: String = "",
    val button2Url: String = "",
    val startTimestamp: Long? = null,
    val endTimestamp: Long? = null,
    val activityType: Int = 0,
    val status: String = "online",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
fun copyWith(
        newName: String? = null,
        newDetails: String? = null,
        newState: String? = null,
        newLargeImage: String? = null,
        newLargeText: String? = null,
        newSmallImage: String? = null,
        newSmallText: String? = null,
        newButton1Label: String? = null,
        newButton1Url: String? = null,
        newButton2Label: String? = null,
        newButton2Url: String? = null,
        newStartTimestamp: Long? = null,
        newEndTimestamp: Long? = null,
        newActivityType: Int? = null,
        newStatus: String? = null,
        newIsFavorite: Boolean? = null
    ): CustomRpcPreset {
        return copy(
            name = newName ?: this.name,
            details = newDetails ?: this.details,
            state = newState ?: this.state,
            largeImage = newLargeImage ?: this.largeImage,
            largeText = newLargeText ?: this.largeText,
            smallImage = newSmallImage ?: this.smallImage,
            smallText = newSmallText ?: this.smallText,
            button1Label = newButton1Label ?: this.button1Label,
            button1Url = newButton1Url ?: this.button1Url,
            button2Label = newButton2Label ?: this.button2Label,
            button2Url = newButton2Url ?: this.button2Url,
            startTimestamp = newStartTimestamp,
            endTimestamp = newEndTimestamp,
            activityType = newActivityType ?: this.activityType,
            status = newStatus ?: this.status,
            isFavorite = newIsFavorite ?: this.isFavorite,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun toPresenceData(): PresenceData = PresenceData(
        name = name,
        type = activityType,
        details = details,
        state = state,
        largeImage = largeImage,
        largeText = largeText,
        smallImage = smallImage,
        smallText = smallText,
        startTimestamp = startTimestamp,
        endTimestamp = endTimestamp,
        status = status,
        enabled = true,
        button1Label = button1Label,
        button1Url = button1Url,
        button2Label = button2Label,
        button2Url = button2Url
    )
}
