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

@Composable
fun OrbitAnimation(
    modifier: Modifier = Modifier,
    orbitCount: Int = 3,
    color: Color = Blurple
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbit")

    val animatedRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 8000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitRotation"
    )

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2

        // Draw orbit rings
        for (i in 1..orbitCount) {
            val radius = 80f * i

            drawCircle(
                color = color.copy(alpha = 0.1f),
                radius = radius,
                center = Offset(centerX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(10f, 10f), 0f
                    )
                )
            )
        }

        // Draw orbiting particles
        for (i in 0 until orbitCount) {
            val radius = 80f * (i + 1)
            val angle = (animatedRotation + i * 120f) * (Math.PI / 180).toFloat()

            val x = centerX + kotlin.math.cos(angle) * radius
            val y = centerY + kotlin.math.sin(angle) * radius

            // Particle trail
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.6f),
                        color.copy(alpha = 0f)
                    ),
                    center = Offset(x, y),
                    radius = 20f
                ),
                radius = 20f,
                center = Offset(x, y)
            )

            // Particle
            drawCircle(
                color = color,
                radius = 6f,
                center = Offset(x, y)
            )
        }
    }
}
