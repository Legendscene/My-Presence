package com.kyrx.mypresence.feature.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyrx.mypresence.ui.components.GlassCard
import com.kyrx.mypresence.ui.components.PremiumSwitch
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Blurple
import com.kyrx.mypresence.ui.theme.GlassAppearance
import com.kyrx.mypresence.ui.theme.Gold
import com.kyrx.mypresence.ui.theme.GoldLight
import com.kyrx.mypresence.ui.theme.Success
import com.kyrx.mypresence.ui.theme.SurfaceBorder
import com.kyrx.mypresence.ui.theme.SurfaceCard
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextTertiary

@Composable
fun PresenceToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    GlassCard(
        appearance = GlassAppearance(
            baseColor = SurfaceCard, alpha = 0.6f,
            borderColor = if (enabled) Gold.copy(alpha = 0.25f) else SurfaceBorder.copy(alpha = 0.2f),
            glowColor = if (enabled) Gold.copy(alpha = 0.1f) else SurfaceBorder.copy(alpha = 0.05f),
            cornerRadius = 18.dp
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (enabled) Modifier.background(
                                brush = Brush.linearGradient(listOf(Gold, GoldLight))
                            ) else Modifier.background(Blurple.copy(alpha = 0.15f))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = if (enabled) Background else Blurple,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("Rich Presence", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.W600)
                    Text(
                        if (enabled) "Broadcasting activity" else "Tap to start",
                        color = if (enabled) Success else TextTertiary,
                        fontSize = 12.sp
                    )
                }
            }
            PremiumSwitch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}
