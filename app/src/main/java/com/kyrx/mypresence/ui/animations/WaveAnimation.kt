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
import androidx.compose.ui.graphics.Color
import com.kyrx.mypresence.ui.theme.Blurple
import kotlin.math.sin

@Composable
fun WaveAnimation(
    modifier: Modifier = Modifier,
    color: Color = Blurple,
    waveCount: Int = 3
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        for (i in 0 until waveCount) {
            val offset = i * 30f
            val alpha = 1f - (i * 0.2f)

            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, centerY)

                for (x in 0..width.toInt() step 5) {
                    val y = centerY + sin(
                        Math.toRadians((x * 0.02 + animatedPhase + offset).toDouble())
                    ).toFloat() * (50f - i * 10f)
                    lineTo(x.toFloat(), y)
                }

                lineTo(width, height)
                lineTo(0f, height)
                close()
            }

            drawPath(
                path = path,
                color = color.copy(alpha = alpha * 0.3f)
            )
        }
    }
}
