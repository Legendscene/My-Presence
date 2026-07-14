package com.kyrx.mypresence.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentMuted,
    onPrimaryContainer = Accent,
    secondary = TextSecondary,
    onSecondary = Color.White,
    secondaryContainer = SurfaceBorder,
    onSecondaryContainer = TextPrimary,
    tertiary = TextTertiary,
    onTertiary = Color.White,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Accent,
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorderLight,
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFF261415),
    onErrorContainer = Error,
    inverseSurface = TextPrimary,
    inverseOnSurface = Background
)

@Composable
fun MyPresenceTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
