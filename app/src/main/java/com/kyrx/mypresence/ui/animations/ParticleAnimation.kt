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
import com.kyrx.mypresence.ui.theme.Primary
import com.kyrx.mypresence.ui.theme.Secondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ParticleAnimation(
    modifier: Modifier = Modifier,
    particleCount: Int = 30,
    colors: List<Color> = listOf(Primary, Secondary)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    val animatedTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 10000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleTime"
    )

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2

        for (i in 0 until particleCount) {
            val angle = (animatedTime + i * (360f / particleCount)) * (Math.PI / 180).toFloat()
            val radius = 100f + (i % 5) * 50f

            val x = centerX + cos(angle) * radius
            val y = centerY + sin(angle) * radius

            val particleSize = 4f + (i % 3) * 2f
            val alpha = 0.3f + (i % 5) * 0.1f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors[i % colors.size].copy(alpha = alpha),
                        Color.Transparent
                    ),
                    center = Offset(x, y),
                    radius = particleSize * 3
                ),
                radius = particleSize * 3,
                center = Offset(x, y)
            )

            drawCircle(
                color = colors[i % colors.size].copy(alpha = alpha),
                radius = particleSize,
                center = Offset(x, y)
            )
        }
    }
}
