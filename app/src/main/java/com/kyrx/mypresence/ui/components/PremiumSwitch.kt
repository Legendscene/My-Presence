package com.kyrx.mypresence.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyrx.mypresence.ui.theme.Accent
import com.kyrx.mypresence.ui.theme.SurfaceBorder
import com.kyrx.mypresence.ui.theme.Surface

@Composable
fun PremiumSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) Accent.copy(alpha = 0.25f) else SurfaceBorder.copy(alpha = 0.3f),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "track"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 18.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "thumb"
    )
    val thumbSize by animateDpAsState(
        targetValue = if (checked) 20.dp else 16.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "thumbSize"
    )

    Box(
        modifier = modifier
            .size(40.dp, 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(trackColor)
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onCheckedChange(!checked) }
                ) else Modifier
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(if (checked) Accent else Surface)
        )
    }
}
