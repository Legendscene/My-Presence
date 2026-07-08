package com.kyrx.mypresence.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Background,
    primaryContainer = GoldContainer,
    onPrimaryContainer = GoldLight,
    secondary = Blurple,
    onSecondary = TextPrimary,
    secondaryContainer = BlurpleContainer,
    onSecondaryContainer = BlurpleLight,
    tertiary = Cyan,
    onTertiary = Background,
    tertiaryContainer = Color(0xFF0F2A3D),
    onTertiaryContainer = CyanLight,
    background = Background,
    onBackground = TextPrimary,
    surface = SurfaceMid,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Gold,
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorder.copy(alpha = 0.5f),
    error = Error,
    onError = TextPrimary,
    errorContainer = ErrorContainer,
    onErrorContainer = Error
)

// Light surface colors
private val LightBackground = Color(0xFFF5F5FA)
private val LightSurfaceCard = Color(0xFFFFFFFF)
private val LightSurfaceBorder = Color(0xFFE0E0EA)
private val LightTextPrimary = Color(0xFF1A1A2E)
private val LightTextSecondary = Color(0xFF6B6B7A)
private val LightTextTertiary = Color(0xFF9A9AB0)

private val LightColorScheme = lightColorScheme(
    primary = Gold,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEE8FF),
    onPrimaryContainer = GoldDark,
    secondary = Blurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E9FF),
    onSecondaryContainer = BlurpleDark,
    tertiary = Cyan,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F4FF),
    onTertiaryContainer = CyanDark,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurfaceCard,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFF0F0F6),
    onSurfaceVariant = LightTextSecondary,
    surfaceTint = Gold,
    outline = LightSurfaceBorder,
    outlineVariant = LightSurfaceBorder.copy(alpha = 0.5f),
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFFFFE8E8),
    onErrorContainer = Error
)

@Composable
fun MyPresenceTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
