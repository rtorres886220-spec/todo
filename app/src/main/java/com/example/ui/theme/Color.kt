package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Quiet Instrument Tokens - Updated for Contrast & Depth
val QuietBackground = Color(0xFF07080B)
val QuietBackgroundLift = Color(0xFF12151C)

// High-legibility text contrast
val QuietTextPrimary = Color(0xFFEDEDF0)
val QuietTextSecondary = Color(0xFFA0A3AE)
val QuietTextTertiary = Color(0xFF6B6E78)

// Single Accent: #7FE3FF (Active and "do now" only)
val QuietAccent = Color(0xFF7FE3FF)
val QuietAccentDim = Color(0x337FE3FF)
val QuietAccentGlow = Color(0x267FE3FF)

// Urgency: #FF7A45 (Overdue only)
val QuietUrgency = Color(0xFFFF7A45)
val QuietUrgencyDim = Color(0x33FF7A45)

// Hairlines & Lifted Glass Highlights
val QuietHairline = Color(0x0FFFFFFF) // rgba(255, 255, 255, 0.06)
val QuietHighlight = Color(0x1AFFFFFF) // rgba(255, 255, 255, 0.10)
val QuietGlassFill = Color(0x0AFFFFFF) // rgba(255, 255, 255, 0.04)

// Swipe wash colors
val SwipeWashToday = Color(0x2E7FE3FF)
val SwipeWashSnooze = Color(0x2E4A4D57)
val SwipeWashDone = Color(0x2EEDEDF0)

// Backward compatibility references for existing code
val AnchorBackground = QuietBackground
val AnchorSurface = QuietBackgroundLift
val AnchorSurfaceVariant = Color(0xFF101218)
val AnchorSurfaceHighlight = Color(0xFF161922)
val AnchorCyan = QuietAccent
val AnchorCyanDim = Color(0xFF5ACFEA)
val AnchorCyanGlow = QuietAccent
val AnchorIndigo = QuietTextTertiary
val AnchorEmerald = Color(0xFFEDEDF0)
val AnchorEmeraldDim = QuietTextSecondary
val AnchorAmber = QuietUrgency
val AnchorRose = QuietUrgency
val AnchorTextPrimary = QuietTextPrimary
val AnchorTextSecondary = QuietTextSecondary
val AnchorTextTertiary = QuietTextTertiary
val AnchorOutline = QuietHairline
val AnchorOutlineVariant = QuietHairline
