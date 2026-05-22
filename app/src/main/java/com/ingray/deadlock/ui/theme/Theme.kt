package com.ingray.deadlock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DeadLockColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = BackgroundDeep,
    primaryContainer = SurfaceContainer,
    onPrimaryContainer = NeonCyan,
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = NeonPurple,
    tertiary = NeonGreen,
    onTertiary = BackgroundDeep,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = NeonRed,
    onError = Color.White
)

@Composable
fun DeadLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DeadLockColorScheme,
        typography = Typography,
        content = content
    )
}