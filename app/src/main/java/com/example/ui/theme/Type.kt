package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.R

val Manrope = FontFamily(
    Font(R.font.manrope, FontWeight.ExtraLight),
    Font(R.font.manrope, FontWeight.Light),
    Font(R.font.manrope, FontWeight.Normal)
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Light),
    Font(R.font.jetbrains_mono, FontWeight.Normal)
)

// Display weight 200 at 48px with -0.02em tracking
val QuietDisplay = TextStyle(
    fontFamily = Manrope,
    fontWeight = FontWeight.ExtraLight,
    fontSize = 48.sp,
    lineHeight = 52.sp,
    letterSpacing = (-0.02).em
)

// Body minimum 15px, weight 300
val QuietBody = TextStyle(
    fontFamily = Manrope,
    fontWeight = FontWeight.Light,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.sp
)

val QuietBodySmall = TextStyle(
    fontFamily = Manrope,
    fontWeight = FontWeight.Light,
    fontSize = 15.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.sp
)

// Mono metadata minimum 11px with 0.14em tracking
val QuietMono = TextStyle(
    fontFamily = JetBrainsMono,
    fontWeight = FontWeight.Light,
    fontSize = 11.sp,
    lineHeight = 15.sp,
    letterSpacing = 0.14.em
)

val QuietMonoMedium = TextStyle(
    fontFamily = JetBrainsMono,
    fontWeight = FontWeight.Light,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.14.em
)

val Typography = Typography(
    displayLarge = QuietDisplay,
    displayMedium = QuietDisplay.copy(fontSize = 40.sp, lineHeight = 44.sp),
    headlineMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraLight,
        fontSize = 24.sp,
        letterSpacing = (-0.02).em
    ),
    titleLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Light,
        fontSize = 18.sp,
        letterSpacing = (-0.01).em
    ),
    titleMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Light,
        fontSize = 16.sp
    ),
    bodyLarge = QuietBody,
    bodyMedium = QuietBodySmall,
    bodySmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Light,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelLarge = QuietMonoMedium,
    labelMedium = QuietMono,
    labelSmall = QuietMono
)
