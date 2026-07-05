package com.kyrx.mypresence.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Error
import com.kyrx.mypresence.ui.theme.Primary
import com.kyrx.mypresence.ui.theme.Surface
import com.kyrx.mypresence.ui.theme.Success
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextSecondary
import com.kyrx.mypresence.ui.theme.Warning

enum class SnackbarType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

@Composable
fun PremiumSnackbar(
    message: String,
    type: SnackbarType = SnackbarType.INFO,
    icon: ImageVector? = null,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (type) {
        SnackbarType.SUCCESS -> Success.copy(alpha = 0.15f)
        SnackbarType.ERROR -> Error.copy(alpha = 0.15f)
        SnackbarType.WARNING -> Warning.copy(alpha = 0.15f)
        SnackbarType.INFO -> Primary.copy(alpha = 0.15f)
    }

    val contentColor = when (type) {
        SnackbarType.SUCCESS -> Success
        SnackbarType.ERROR -> Error
        SnackbarType.WARNING -> Warning
        SnackbarType.INFO -> Primary
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
        ) + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor
                    )

                    Spacer(modifier = Modifier.width(12.dp))
                }

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
