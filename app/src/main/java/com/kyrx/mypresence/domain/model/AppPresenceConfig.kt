package com.kyrx.mypresence.domain.model

import androidx.compose.runtime.Stable

@Stable
data class AppPresenceConfig(
    val packageName: String,
    val name: String = "",
    val details: String = "",
    val state: String = "",
    val activityType: Int = -1,
    val privacySafe: Boolean = true
)
