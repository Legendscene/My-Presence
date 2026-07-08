package com.kyrx.mypresence.feature.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyrx.mypresence.feature.dashboard.model.QuickAction
import com.kyrx.mypresence.ui.components.GlassCard
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Blurple
import com.kyrx.mypresence.ui.theme.Cyan
import com.kyrx.mypresence.ui.theme.GlassAppearance
import com.kyrx.mypresence.ui.theme.Gold
import com.kyrx.mypresence.ui.theme.SurfaceBorder
import com.kyrx.mypresence.ui.theme.SurfaceCard
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextTertiary

@Composable
fun QuickActionsSection(
    actions: List<QuickAction>,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        appearance = GlassAppearance(
            baseColor = SurfaceCard, alpha = 0.55f,
            borderColor = Gold.copy(alpha = 0.15f),
            glowColor = Gold.copy(alpha = 0.05f), cornerRadius = 18.dp
        )
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            actions.forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = action.onClick
                        )
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Gold.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            action.icon,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            action.label,
                            color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.W600
                        )
                        Text(
                            action.subtitle,
                            color = TextTertiary, fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
