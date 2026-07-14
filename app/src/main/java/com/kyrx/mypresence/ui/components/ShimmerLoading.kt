package com.kyrx.mypresence.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyrx.mypresence.ui.theme.Surface
import com.kyrx.mypresence.ui.theme.SurfaceBorder

private val shimmerCardColors = listOf(
    Surface.copy(alpha = 0.6f),
    SurfaceBorder.copy(alpha = 0.4f),
    Surface.copy(alpha = 0.6f)
)

private val shimmerLineColors = listOf(
    Surface.copy(alpha = 0.6f),
    SurfaceBorder.copy(alpha = 0.3f),
    Surface.copy(alpha = 0.6f)
)

private val shimmerAvatarColors = listOf(
    Surface.copy(alpha = 0.6f),
    SurfaceBorder.copy(alpha = 0.3f),
    Surface.copy(alpha = 0.6f)
)

@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = remember(translateAnimation) {
        Brush.linearGradient(
            colors = shimmerCardColors,
            start = Offset(translateAnimation - 200f, 0f),
            end = Offset(translateAnimation, 0f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
    )
}

@Composable
fun ShimmerLine(
    modifier: Modifier = Modifier,
    width: Dp = 200.dp,
    height: Dp = 14.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmerLine")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerLineTranslate"
    )

    val brush = remember(translateAnimation) {
        Brush.linearGradient(
            colors = shimmerLineColors,
            start = Offset(translateAnimation - 100f, 0f),
            end = Offset(translateAnimation, 0f)
        )
    }

    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(height / 2))
            .background(brush)
    )
}

@Composable
fun ShimmerAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmerAvatar")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerAvatarTranslate"
    )

    val brush = remember(translateAnimation) {
        Brush.linearGradient(
            colors = shimmerAvatarColors,
            start = Offset(translateAnimation - 50f, 0f),
            end = Offset(translateAnimation, 0f)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush)
    )
}
