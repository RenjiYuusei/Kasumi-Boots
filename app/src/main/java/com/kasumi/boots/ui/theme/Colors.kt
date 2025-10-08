package com.kasumi.boots.ui.theme

import android.graphics.Color

object Colors {
    // Background colors - Modern dark theme with subtle gradient effect
    val Background = Color.parseColor("#0D0D12")
    val BackgroundSecondary = Color.parseColor("#1A1A24")
    val Surface = Color.parseColor("#1C1C28")
    val SurfaceElevated = Color.parseColor("#252535")
    val SurfaceBorder = Color.parseColor("#2F2F42")
    val SurfaceGlow = Color.parseColor("#1A3A3A")
    
    // Primary colors - Vibrant cyan-green gradient
    val Primary = Color.parseColor("#00FFB3")
    val PrimaryDark = Color.parseColor("#00D99A")
    val PrimaryLight = Color.parseColor("#5AFFCC")
    val PrimaryGradientStart = Color.parseColor("#00FFB3")
    val PrimaryGradientEnd = Color.parseColor("#00D4FF")
    
    // Accent colors
    val Accent = Color.parseColor("#00D4FF")
    val AccentPurple = Color.parseColor("#A855F7")
    val AccentPink = Color.parseColor("#EC4899")
    
    // Text colors - Enhanced contrast
    val TextPrimary = Color.parseColor("#FFFFFF")
    val TextSecondary = Color.parseColor("#C4C4D4")
    val TextTertiary = Color.parseColor("#8A8A9E")
    val TextMuted = Color.parseColor("#5A5A6E")
    
    // Status colors - More vibrant
    val Success = Color.parseColor("#00FFB3")
    val SuccessGlow = Color.parseColor("#00FFB333")
    val Error = Color.parseColor("#FF5757")
    val ErrorGlow = Color.parseColor("#FF575733")
    val Warning = Color.parseColor("#FFB800")
    val WarningGlow = Color.parseColor("#FFB80033")
    val Info = Color.parseColor("#00D4FF")
    val InfoGlow = Color.parseColor("#00D4FF33")
    
    // Special colors
    val Discord = Color.parseColor("#5865F2")
    val DiscordHover = Color.parseColor("#4752C4")
    val DiscordGlow = Color.parseColor("#5865F233")
    
    // Button states
    val ButtonEnabled = Primary
    val ButtonDisabled = Color.parseColor("#2A2A3A")
    val ButtonLoading = PrimaryDark
    val ButtonHover = PrimaryLight
    
    // Progress colors
    val ProgressTrack = Color.parseColor("#2A2A3A")
    val ProgressIndicator = Primary
    val ProgressGlow = Color.parseColor("#00FFB355")
    
    // Card colors
    val CardBackground = Surface
    val CardBorder = SurfaceBorder
    val CardElevated = SurfaceElevated
    val CardGlow = Color.parseColor("#00FFB311")
    
    // Shadow colors for depth
    val Shadow = Color.parseColor("#00000055")
    val ShadowLight = Color.parseColor("#00000033")
    val Glow = Color.parseColor("#00FFB322")
}
