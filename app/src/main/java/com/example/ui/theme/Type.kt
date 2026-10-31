package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Apple Human Interface Design Typography System:
 * - Primary UI System Sans: San Francisco (SF Pro), represented via Inter (open-source SF Pro standard)
 * - Monospace Telemetry / Code: SF Mono, represented via JetBrains Mono (clean slashed zero, tabular figures)
 */
val SFPro = FontFamily(
    Font(R.font.inter, FontWeight.Light),
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val SFMono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Light),
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

// Aliases for seamless backward compatibility across all existing UI components
val AppleSystemSans = SFPro
val PlusJakartaSans = SFPro
val AppleSystemMono = SFMono
val GeistMono = SFMono

// Apple HIG Display & Numerals
val DisplayNumeralLarge = TextStyle(
    fontFamily = SFPro,
    fontWeight = FontWeight.SemiBold,
    fontSize = 54.sp,
    lineHeight = 58.sp,
    letterSpacing = (-0.025).em
)

val DisplayNumeralMedium = TextStyle(
    fontFamily = SFPro,
    fontWeight = FontWeight.SemiBold,
    fontSize = 42.sp,
    lineHeight = 46.sp,
    letterSpacing = (-0.02).em
)

// Apple HIG Large Title (34-40sp, tracking -0.02em)
val ScreenTitleLarge = TextStyle(
    fontFamily = SFPro,
    fontWeight = FontWeight.Bold,
    fontSize = 38.sp,
    lineHeight = 44.sp,
    letterSpacing = (-0.02).em
)

// Apple HIG Title 1 & Title 2 (20-24sp)
val TitleLarge = TextStyle(
    fontFamily = SFPro,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.015).em
)

val TitleMedium = TextStyle(
    fontFamily = SFPro,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    letterSpacing = (-0.01).em
)

val TitleSmall = TextStyle(
    fontFamily = SFPro,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = (-0.005).em
)

// Apple HIG Body (16-17sp, clean legibility)
val BodyLarge = TextStyle(
    fontFamily = SFPro,
    fontWeight = FontWeight.Normal,
    fontSize = 17.sp,
    lineHeight = 24.sp,
    letterSpacing = (-0.005).em
)

val BodyRegular = TextStyle(
    fontFamily = SFPro,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.sp
)

// Apple HIG Footnote & Caption in SF Mono (tabular, uppercase tracking 0.08em)
val MetadataMono = TextStyle(
    fontFamily = SFMono,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 15.sp,
    letterSpacing = 0.08.em
)

val MetadataMonoMedium = TextStyle(
    fontFamily = SFMono,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.08.em
)

val Typography = Typography(
    displayLarge = DisplayNumeralLarge,
    displayMedium = DisplayNumeralMedium,
    headlineMedium = ScreenTitleLarge,
    headlineSmall = TextStyle(
        fontFamily = SFPro,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = (-0.015).em
    ),
    titleLarge = TitleLarge,
    titleMedium = TitleMedium,
    titleSmall = TitleSmall,
    bodyLarge = BodyLarge,
    bodyMedium = BodyRegular,
    bodySmall = BodyRegular,
    labelLarge = MetadataMonoMedium,
    labelMedium = MetadataMono,
    labelSmall = MetadataMono
)
