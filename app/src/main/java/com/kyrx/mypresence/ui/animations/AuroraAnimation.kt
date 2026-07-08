package com.kyrx.mypresence.ui.animations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.kyrx.mypresence.ui.theme.Blurple
import com.kyrx.mypresence.ui.theme.Cyan

@Composable
fun AuroraAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    val animatedOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 15000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraOffset1"
    )

    val animatedOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 18000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraOffset2"
    )

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height

        // Aurora layer 1
        val gradient1 = Brush.linearGradient(
            colors = listOf(
                Blurple.copy(alpha = 0.3f),
                Cyan.copy(alpha = 0.2f),
                Color.Transparent
            ),
            start = Offset(0f, height * animatedOffset1 / 100),
            end = Offset(width, height * (animatedOffset1 + 50) / 100)
        )

        // Aurora layer 2
        val gradient2 = Brush.linearGradient(
            colors = listOf(
                Cyan.copy(alpha = 0.2f),
                Blurple.copy(alpha = 0.3f),
                Color.Transparent
            ),
            start = Offset(width, height * animatedOffset2 / 100),
            end = Offset(0f, height * (animatedOffset2 + 50) / 100)
        )

        drawRect(brush = gradient1)
        drawRect(brush = gradient2)
    }
}
