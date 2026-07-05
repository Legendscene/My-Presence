package com.kyrx.mypresence.ui.theme

import androidx.compose.ui.graphics.Color

// Background
val Background = Color(0xFF09090B)
val BackgroundElevated = Color(0xFF0F0F12)

// Surface
val Surface = Color(0xFF111827)
val SurfaceElevated = Color(0xFF1A1F2E)
val SurfaceHover = Color(0xFF1F2537)
val SurfaceBorder = Color(0xFF252B3B)

// Primary Accent - Blurple
val Primary = Color(0xFF5865F2)
val PrimaryLight = Color(0xFF7B83F5)
val PrimaryDark = Color(0xFF4752C4)
val PrimaryContainer = Color(0xFF1E2340)

// Secondary Accent - Cyan
val Secondary = Color(0xFF4FD1FF)
val SecondaryLight = Color(0xFF7DDEFF)
val SecondaryDark = Color(0xFF2BA3E0)
val SecondaryContainer = Color(0xFF0F2A3D)

// Text
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)
val TextDisabled = Color(0xFF475569)

// Status
val Success = Color(0xFF22C55E)
val SuccessContainer = Color(0xFF0F2918)
val Warning = Color(0xFFF97316)
val WarningContainer = Color(0xFF2D1B0E)
val Error = Color(0xFFEF4444)
val ErrorContainer = Color(0xFF2D1215)

// Gradients
val GradientPrimary = listOf(Primary, Secondary)
val GradientSurface = listOf(Surface, SurfaceElevated)
val GradientCard = listOf(Surface, Background)

// Overlays
val OverlayLight = Color(0x0DFFFFFF)
val OverlayMedium = Color(0x1AFFFFFF)
val OverlayHeavy = Color(0x33FFFFFF)

// Legacy Discord colors for compatibility
val DiscordDark = Surface
val DiscordDarker = Background
val DiscordMid = SurfaceBorder
val DiscordLight = SurfaceElevated
val DiscordText = TextPrimary
val DiscordTextMuted = TextSecondary
val DiscordTextFaint = TextTertiary
val DiscordBlurple = Primary
val DiscordBlurpleDark = PrimaryDark
val DiscordGreen = Success
val DiscordYellow = Warning
val DiscordRed = Error
val DiscordFuchsia = Color(0xFFEB459E)

val DiscordLightBg = Color(0xFFFFFFFF)
val DiscordLightSurface = Color(0xFFF2F3F5)
val DiscordLightMid = Color(0xFFE3E5E8)
val DiscordLightText = Color(0xFF060607)
val DiscordLightTextMuted = Color(0xFF5C5E66)

val GradientStart = Primary
val GradientEnd = Secondary
