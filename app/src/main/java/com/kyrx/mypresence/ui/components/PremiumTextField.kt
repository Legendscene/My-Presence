package com.kyrx.mypresence.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyrx.mypresence.ui.theme.Accent
import com.kyrx.mypresence.ui.theme.Dimens
import com.kyrx.mypresence.ui.theme.SurfaceBorder
import com.kyrx.mypresence.ui.theme.Surface
import com.kyrx.mypresence.ui.theme.TextPrimary
import com.kyrx.mypresence.ui.theme.TextTertiary

@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    label: String? = null
) {
    if (label != null) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.W500,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, color = TextTertiary, fontSize = 14.sp) }
        } else null,
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(icon, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(20.dp))
            }
        },
        trailingIcon = trailingIcon?.let { icon ->
            {
                if (onTrailingIconClick != null) {
                    IconButton(onClick = onTrailingIconClick) {
                        Icon(icon, contentDescription = null, tint = TextTertiary)
                    }
                } else {
                    Icon(icon, contentDescription = null, tint = TextTertiary)
                }
            }
        },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        shape = RoundedCornerShape(Dimens.inputCorner),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Surface,
            unfocusedContainerColor = Surface,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = Accent,
            focusedIndicatorColor = Accent.copy(alpha = 0.4f),
            unfocusedIndicatorColor = SurfaceBorder.copy(alpha = 0.2f),
            disabledContainerColor = Surface.copy(alpha = 0.5f),
            disabledTextColor = TextTertiary
        )
    )
}
