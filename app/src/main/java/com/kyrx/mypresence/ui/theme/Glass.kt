package com.kyrx.mypresence.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Stable
data class GlassAppearance(
    val baseColor: Color = SurfaceCard,
    val alpha: Float = 0.7f,
    val borderColor: Color = SurfaceBorder.copy(alpha = 0.3f),
    val borderWidth: Dp = 0.5.dp,
    val glowColor: Color = Gold.copy(alpha = 0.06f),
    val glowRadius: Dp = 30.dp,
    val cornerRadius: Dp = 16.dp,
    val hasShadow: Boolean = true
)

@Stable
fun Modifier.glassBackground(appearance: GlassAppearance = GlassAppearance()): Modifier = this.drawBehind {
    val cornerPx = appearance.cornerRadius.toPx()

    drawRoundRect(
        color = appearance.baseColor.copy(alpha = appearance.alpha),
        cornerRadius = CornerRadius(cornerPx, cornerPx),
        size = size
    )

    if (appearance.glowColor.alpha > 0f && appearance.glowRadius > 0.dp) {
        val glowPx = appearance.glowRadius.toPx()
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(appearance.glowColor, Color.Transparent),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = min(size.width, size.height) / 2f + glowPx.coerceAtLeast(0f)
            ),
            cornerRadius = CornerRadius(cornerPx, cornerPx),
            size = size
        )
    }
}

@Stable
fun Modifier.glassBorder(appearance: GlassAppearance = GlassAppearance()): Modifier = this.drawBehind {
    val cornerPx = appearance.cornerRadius.toPx()
    val strokePx = appearance.borderWidth.toPx()

    drawRoundRect(
        color = appearance.borderColor,
        cornerRadius = CornerRadius(cornerPx, cornerPx),
        size = Size(size.width - strokePx * 2, size.height - strokePx * 2),
        topLeft = Offset(strokePx, strokePx),
        style = Stroke(width = strokePx)
    )
}

@Stable
fun Modifier.glassCard(appearance: GlassAppearance = GlassAppearance()): Modifier = this
    .clip(RoundedCornerShape(appearance.cornerRadius))
    .glassBackground(appearance)
    .glassBorder(appearance)
