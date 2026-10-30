package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SoftDimensionalDarkColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = GoldOnDarkText,
    primaryContainer = Surface2,
    onPrimaryContainer = AccentGold,
    secondary = TextSecondary,
    onSecondary = SoftDarkBackground,
    secondaryContainer = Surface1,
    onSecondaryContainer = TextPrimary,
    tertiary = TextTertiary,
    onTertiary = TextPrimary,
    background = SoftDarkBackground,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    outline = HairlineColor,
    outlineVariant = SurfaceHighlight,
    error = UrgencyCoral,
    onError = Color(0xFF280B04)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SoftDimensionalDarkColorScheme,
        typography = Typography,
        content = content
    )
}
