package com.ingray.deadlock.ui.theme

import androidx.compose.ui.graphics.Color

// Quantum Obsidian Palette
val ObsidianBlack = Color(0xFF040405)
val BackgroundDark = Color(0xFF08080C)
val SurfaceDark = Color(0xFF0E0E16)
val SurfaceElevated = Color(0xFF141421)
val SurfaceVariant = Color(0xFF1B1B2B)

// Live Neon Accents
val NeonCyan = Color(0xFF00F2FF)      // Productivity / Focus
val NeonPurple = Color(0xFFB026FF)    // Digital Wellbeing / Screen Time
val NeonRed = Color(0xFFFF2D55)       // Blocked / No Mercy
val NeonGreen = Color(0xFF00FF87)     // Active / Completed
val NeonAmber = Color(0xFFFFB800)     // Warnings / Streaks

// Subtle Accents
val GlassWhite = Color(0xFFFFFFFF).copy(alpha = 0.05f)
val GlassCyan = Color(0xFF00F2FF).copy(alpha = 0.1f)
val GlassRed = Color(0xFFFF2D55).copy(alpha = 0.1f)

// Text Hierarchy
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8E8E9F)
val TextTertiary = Color(0xFF5A5A6E)
val TextDisabled = Color(0xFF3F3F4D)

// Borders
val BorderSubtle = Color(0xFF222233)
val BorderMedium = Color(0xFF2D2D44)
val BorderBright = Color(0xFF3D3D5C)

// Constants for legacy compatibility
val BackgroundDeep = ObsidianBlack
val SurfaceContainer = SurfaceVariant
val BorderGlow = NeonCyan.copy(alpha = 0.3f)
