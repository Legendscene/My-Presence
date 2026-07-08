package com.kyrx.mypresence.feature.dashboard.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.VideogameAsset
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
import com.kyrx.mypresence.domain.model.AppInfo
import com.kyrx.mypresence.ui.components.GlassCard
import com.kyrx.mypresence.ui.theme.Blurple
import com.kyrx.mypresence.ui.theme.Cyan
import com.kyrx.mypresence.ui.theme.GlassAppearance
import com.kyrx.mypresence.ui.theme.Gold
import com.kyrx.mypresence.ui.theme.Success
import com.kyrx.mypresence.ui.theme.SurfaceCard
import com.kyrx.mypresence.ui.theme.SurfaceLight
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextSecondary
import com.kyrx.mypresence.ui.theme.TextTertiary

@Composable
fun CurrentActivity(
    presenceEnabled: Boolean,
    detectedApp: AppInfo?
) {
    GlassCard(
        appearance = GlassAppearance(
            baseColor = SurfaceCard, alpha = 0.55f,
            borderColor = if (presenceEnabled) Cyan.copy(alpha = 0.15f) else Gold.copy(alpha = 0.1f),
            glowColor = if (presenceEnabled) Cyan.copy(alpha = 0.08f) else Gold.copy(alpha = 0.04f),
            cornerRadius = 18.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (presenceEnabled) Modifier.background(
                            brush = Brush.linearGradient(listOf(Cyan.copy(alpha = 0.3f), Blurple.copy(alpha = 0.3f)))
                        ) else Modifier.background(SurfaceLight.copy(alpha = 0.3f))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.VideogameAsset,
                    contentDescription = null,
                    tint = if (presenceEnabled) Cyan else TextTertiary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                val displayName = detectedApp?.appName ?: "No active presence"
                Text(
                    displayName,
                    color = if (presenceEnabled) TextPrimary else TextSecondary,
                    fontSize = 16.sp, fontWeight = FontWeight.W600
                )
                if (detectedApp != null) {
                    Text("Running in foreground", color = Success, fontSize = 12.sp, fontWeight = FontWeight.W500)
                }
            }
        }
    }
}
