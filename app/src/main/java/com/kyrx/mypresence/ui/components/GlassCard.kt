package com.kyrx.mypresence.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding as layoutPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyrx.mypresence.ui.theme.Dimens

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.cardCorner),
        color = com.kyrx.mypresence.ui.theme.Surface,
        tonalElevation = 0.dp
    ) {
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.cardCorner),
        color = com.kyrx.mypresence.ui.theme.Surface,
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.layoutPadding(contentPadding)) {
            content()
        }
    }
}
