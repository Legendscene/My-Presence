package com.kyrx.mypresence.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyrx.mypresence.R
import com.kyrx.mypresence.ui.theme.Accent
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedLogo(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    orbitColor: Color = Accent,
    showOrbits: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (showOrbits) {
            OrbitParticles(
                rotation = rotation,
                color = orbitColor,
                modifier = Modifier.size(size * 1.8f)
            )
        }

        Box(
            modifier = Modifier
                .size(size)
                .scale(pulse)
                .graphicsLayer {
                    shadowElevation = 12f * glowAlpha
                    shape = CircleShape
                    clip = true
                }
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Accent, Accent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = "My Presence",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.9f }
            )
        }
    }
}

@Composable
private fun OrbitParticles(
    rotation: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val orbitRadius = cx * 0.6f
        val particleCount = 3

        for (i in 0 until particleCount) {
            val angle = (rotation + i * 120f) * (Math.PI.toFloat() / 180f)
            val px = cx + cos(angle) * orbitRadius
            val py = cy + sin(angle) * orbitRadius

            drawCircle(
                color = color.copy(alpha = 0.15f),
                radius = 12f,
                center = Offset(px, py),
                style = Stroke(width = 2f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.6f),
                        color.copy(alpha = 0f)
                    ),
                    center = Offset(px, py),
                    radius = 10f
                ),
                radius = 10f,
                center = Offset(px, py)
            )

            drawCircle(
                color = color,
                radius = 3f,
                center = Offset(px, py)
            )
        }

        drawCircle(
            color = color.copy(alpha = 0.06f),
            radius = orbitRadius,
            center = Offset(cx, cy),
            style = Stroke(
                width = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(8f, 8f), 0f
                )
            )
        )
    }
}
