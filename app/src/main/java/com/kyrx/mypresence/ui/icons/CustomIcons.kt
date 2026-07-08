package com.kyrx.mypresence.ui.icons

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.kyrx.mypresence.R

@Composable
fun DiscordIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        painter = painterResource(R.drawable.ic_discord),
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}

@Composable
fun GoogleIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Image(
        painter = painterResource(R.drawable.ic_google),
        contentDescription = null,
        modifier = modifier
    )
}
