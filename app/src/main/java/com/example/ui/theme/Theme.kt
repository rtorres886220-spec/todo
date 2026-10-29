package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val QuietDarkColorScheme = darkColorScheme(
    primary = QuietAccent,
    onPrimary = Color(0xFF041E26),
    primaryContainer = Color(0xFF0C2B34),
    onPrimaryContainer = QuietAccent,
    secondary = QuietTextSecondary,
    onSecondary = QuietBackground,
    secondaryContainer = QuietBackgroundLift,
    onSecondaryContainer = QuietTextPrimary,
    tertiary = QuietTextTertiary,
    onTertiary = QuietTextPrimary,
    background = QuietBackground,
    onBackground = QuietTextPrimary,
    surface = QuietBackgroundLift,
    onSurface = QuietTextPrimary,
    surfaceVariant = Color(0xFF101217),
    onSurfaceVariant = QuietTextSecondary,
    outline = QuietHairline,
    outlineVariant = QuietHairline,
    error = QuietUrgency,
    onError = Color(0xFF280B04)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = QuietDarkColorScheme,
        typography = Typography,
        content = content
    )
}
