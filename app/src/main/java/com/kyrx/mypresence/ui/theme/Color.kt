package com.kyrx.mypresence.ui.theme

import androidx.compose.ui.graphics.Color

// ── Base Palette ────────────────────────────────────────────────────
// Deep Indigo Canvas
val Background = Color(0xFF06060E)
val BackgroundElevated = Color(0xFF0A0A16)
val SurfaceDark = Color(0xFF0E0E1C)
val SurfaceMid = Color(0xFF141426)
val SurfaceLight = Color(0xFF1A1A30)
val SurfaceCard = Color(0xFF1E1E36)
val SurfaceBorder = Color(0xFF2A2A46)
val SurfaceHover = Color(0xFF323252)

// Violet Accent (Primary)
val Gold = Color(0xFF7C5CFC)
val GoldLight = Color(0xFFA78BFA)
val GoldDark = Color(0xFF5B3ED4)
val GoldSoft = Color(0xFF1C1440)
val GoldContainer = Color(0xFF282050)

// Discord Blurple (Secondary)
val Blurple = Color(0xFF5865F2)
val BlurpleLight = Color(0xFF7B83F5)
val BlurpleDark = Color(0xFF4752C4)
val BlurpleContainer = Color(0xFF1E2340)

// Cyan Accent
val Cyan = Color(0xFF4FD1FF)
val CyanLight = Color(0xFF7DDEFF)
val CyanDark = Color(0xFF2BA3E0)

// Text Hierarchy
val TextPrimary = Color(0xFFF2F2F4)
val TextSecondary = Color(0xFFA0A0B0)
val TextTertiary = Color(0xFF6B6B7A)
val TextDisabled = Color(0xFF3F3F4A)

// Status
val Success = Color(0xFF22C55E)
val SuccessContainer = Color(0xFF0D2E1A)
val Warning = Color(0xFFF59E0B)
val WarningContainer = Color(0xFF2E220A)
val Error = Color(0xFFEF4444)
val ErrorContainer = Color(0xFF2E1014)

// Glass / Translucent
val GlassWhite = Color(0x14FFFFFF)
val GlassWhiteStrong = Color(0x1AFFFFFF)
val GlassViolet = Color(0x0D7C5CFC)
val GlassBlurple = Color(0x0D5865F2)

// Shadows
val ShadowDark = Color(0x40000000)
val ShadowViolet = Color(0x207C5CFC)

// Gradients
val GradientGold = listOf(Gold, GoldLight)
val GradientGoldDark = listOf(GoldDark, Gold)
val GradientBlurple = listOf(Blurple, BlurpleLight)
val GradientDark = listOf(SurfaceDark, SurfaceMid)
val GradientCard = listOf(SurfaceCard, SurfaceLight)
val GradientText = listOf(Gold, Blurple)
