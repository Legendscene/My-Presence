package com.kyrx.mypresence.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.kyrx.mypresence.ui.theme.Gold
import com.kyrx.mypresence.ui.theme.GoldLight

@Composable
fun GradientText(
    text: String,
    modifier: Modifier = Modifier,
    gradient: List<Color> = listOf(Gold, GoldLight),
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.W600
) {
    val annotatedString = buildAnnotatedString {
        withStyle(
            SpanStyle(
                brush = Brush.horizontalGradient(gradient),
                fontWeight = fontWeight,
                fontSize = fontSize
            )
        ) {
            append(text)
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier
    )
}
