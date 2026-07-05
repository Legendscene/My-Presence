package com.kyrx.mypresence.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PrimaryLight,
    secondary = Secondary,
    onSecondary = Background,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = SecondaryLight,
    tertiary = Success,
    onTertiary = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = Error,
    onError = TextPrimary,
    errorContainer = ErrorContainer,
    onErrorContainer = Error
)

@Composable
fun MyPresenceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
