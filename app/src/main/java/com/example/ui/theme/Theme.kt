package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AnchorCyan,
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = AnchorCyanGlow,
    secondary = AnchorIndigo,
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Color(0xFFC7D2FE),
    tertiary = AnchorEmerald,
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = AnchorBackground,
    onBackground = AnchorTextPrimary,
    surface = AnchorSurface,
    onSurface = AnchorTextPrimary,
    surfaceVariant = AnchorSurfaceVariant,
    onSurfaceVariant = AnchorTextSecondary,
    outline = AnchorOutline,
    outlineVariant = AnchorOutlineVariant,
    error = AnchorRose,
    onError = Color(0xFF4C0519)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Dark mode by default per user request
    dynamicColor: Boolean = false, // Keep Anchor custom dark theme consistent
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
