package com.kyrx.mypresence.feature.dashboard.model

data class QuickAction(
    val id: String,
    val label: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)
