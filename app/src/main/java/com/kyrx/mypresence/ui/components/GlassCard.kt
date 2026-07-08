package com.kyrx.mypresence.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyrx.mypresence.ui.theme.GlassAppearance
import com.kyrx.mypresence.ui.theme.Gold
import com.kyrx.mypresence.ui.theme.SurfaceBorder
import com.kyrx.mypresence.ui.theme.SurfaceCard
import com.kyrx.mypresence.ui.theme.glassBackground
import com.kyrx.mypresence.ui.theme.glassBorder

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    appearance: GlassAppearance = GlassAppearance(
        baseColor = SurfaceCard,
        alpha = 0.6f,
        borderColor = SurfaceBorder.copy(alpha = 0.2f),
        glowColor = Gold.copy(alpha = 0.05f),
        cornerRadius = 18.dp
    ),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(appearance.cornerRadius))
            .glassBackground(appearance)
            .glassBorder(appearance)
            .padding(16.dp),
        content = content
    )
}
