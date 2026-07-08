package com.kyrx.mypresence.ui.animation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.AnimationSpec

object SpringAnimations {
    val gentle: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val snappy: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val responsive: AnimationSpec<Float> = spring(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMedium
    )

    val stiff: AnimationSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessHigh
    )

    val elastic: AnimationSpec<Float> = spring(
        dampingRatio = 0.4f,
        stiffness = Spring.StiffnessMediumLow
    )
}
