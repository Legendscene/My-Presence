package com.kyrx.mypresence.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun StaggeredReveal(
    index: Int,
    modifier: Modifier = Modifier,
    delayMs: Long = 80L,
    visible: Boolean = true,
    content: @Composable () -> Unit
) {
    var visibleState by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(index * delayMs)
            visibleState = true
        } else {
            visibleState = false
        }
    }

    AnimatedVisibility(
        visible = visibleState,
        enter = fadeIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialOffsetY = { it / 4 }
        ),
        modifier = modifier
    ) {
        content()
    }
}
