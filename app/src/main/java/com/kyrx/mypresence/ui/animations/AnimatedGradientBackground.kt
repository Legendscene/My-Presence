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
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Blurple, Cyan)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")

    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 8000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    val brush = Brush.linearGradient(
        colors = colors,
        start = Offset(animatedOffset, 0f),
        end = Offset(animatedOffset + 500f, 1000f)
    )

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        drawRect(brush = brush)
    }
}

@Composable
fun AnimatedMeshGradient(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")

    val animatedX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 10000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meshX"
    )

    val animatedY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 12000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meshY"
    )

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val gradient1 = Brush.radialGradient(
            colors = listOf(
                Blurple.copy(alpha = 0.4f),
                Color.Transparent
            ),
            center = Offset(size.width * animatedX / 100, size.height * animatedY / 100),
            radius = size.width * 0.5f
        )

        val gradient2 = Brush.radialGradient(
            colors = listOf(
                Cyan.copy(alpha = 0.3f),
                Color.Transparent
            ),
            center = Offset(
                size.width * (100 - animatedX) / 100,
                size.height * (100 - animatedY) / 100
            ),
            radius = size.width * 0.4f
        )

        drawRect(brush = gradient1)
        drawRect(brush = gradient2)
    }
}
