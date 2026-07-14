package com.kyrx.mypresence.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyrx.mypresence.ui.theme.Accent
import com.kyrx.mypresence.ui.theme.Success

@Composable
fun AnimatedPresenceIndicator(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp
) {
    val transition = rememberInfiniteTransition(label = "presenceIndicator")
    val pulse by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "presencePulse"
    )

    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "presenceAlpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(if (isOnline) pulse else 1f)
            .clip(CircleShape)
            .background(
                if (isOnline) Success.copy(alpha = if (isOnline) 1f else alpha)
                else Color.Gray.copy(alpha = 0.4f)
            )
    )

    if (isOnline) {
        Box(
            modifier = Modifier
                .size(size * 2.5f)
                .scale(pulse)
                .clip(CircleShape)
                .background(Success.copy(alpha = alpha * 0.3f))
        )
    }
}
