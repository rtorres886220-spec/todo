package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Design language: "soft dimensional dark"
// Background: #08080A with a very faint radial lift (#101014) behind hero.
val SoftDarkBackground = Color(0xFF08080A)
val SoftDarkRadialLift = Color(0xFF101014)

// Surface tokens
val Surface1 = Color(0xFF131316)
val Surface2 = Color(0xFF1B1B20)

// Raised surface highlight: inset 0 1px 0 rgba(255,255,255,0.07)
val SurfaceHighlight = Color(0x12FFFFFF)
val HairlineColor = Color(0x10FFFFFF)

// Text tokens: primary #F2EFEA (warm off-white), secondary #A5A29B, tertiary #6E6C67
val TextPrimary = Color(0xFFF2EFEA)
val TextSecondary = Color(0xFFA5A29B)
val TextTertiary = Color(0xFF6E6C67)

// Primary accent: Champagne gold #E6C98A (replaces old cyan everywhere)
val AccentGold = Color(0xFFE6C98A)
val AccentGoldDim = Color(0x33E6C98A)
val AccentGoldGlow = Color(0x26E6C98A)
val GoldOnDarkText = Color(0xFF1A1407)

// Urgency: ember coral #FF7A5C (for overdue only)
val UrgencyCoral = Color(0xFFFF7A5C)
val UrgencyCoralDim = Color(0x33FF7A5C)

// Category tints: deep, desaturated dark versions of soft pastels
// Each has matching brighter foreground (icon and date text) at ~75% lightness
val TintOrchidStart = Color(0xFF2A1F2B)
val TintOrchidEnd = Color(0xFF1F1822)
val TintOrchidFg = Color(0xFFE2A9E8)

val TintSageStart = Color(0xFF1D2620)
val TintSageEnd = Color(0xFF171E1A)
val TintSageFg = Color(0xFFA9D8B5)

val TintApricotStart = Color(0xFF2C221B)
val TintApricotEnd = Color(0xFF221A15)
val TintApricotFg = Color(0xFFE6B798)

val TintSlateStart = Color(0xFF1C2230)
val TintSlateEnd = Color(0xFF161B26)
val TintSlateFg = Color(0xFFA1B9DF)

val TintRoseStart = Color(0xFF2B1E22)
val TintRoseEnd = Color(0xFF21171A)
val TintRoseFg = Color(0xFFE4A2AF)

// Swipe wash colors: gold for Today, slate for snooze, soft white for done
val SwipeWashToday = Color(0x2EE6C98A)
val SwipeWashSnooze = Color(0x2E1C2230)
val SwipeWashDone = Color(0x2EF2EFEA)

// Backward compatibility references for existing code
val QuietBackground = SoftDarkBackground
val QuietBackgroundLift = Surface1
val QuietAccent = AccentGold
val QuietAccentDim = AccentGoldDim
val QuietAccentGlow = AccentGoldGlow
val QuietUrgency = UrgencyCoral
val QuietUrgencyDim = UrgencyCoralDim
val QuietHairline = HairlineColor
val QuietHighlight = SurfaceHighlight
val QuietGlassFill = Color(0x0AFFFFFF)
val QuietTextPrimary = TextPrimary
val QuietTextSecondary = TextSecondary
val QuietTextTertiary = TextTertiary

val AnchorBackground = SoftDarkBackground
val AnchorSurface = Surface1
val AnchorSurfaceVariant = Surface2
val AnchorSurfaceHighlight = SurfaceHighlight
val AnchorCyan = AccentGold
val AnchorCyanDim = AccentGoldDim
val AnchorCyanGlow = AccentGoldGlow
val AnchorIndigo = TextTertiary
val AnchorEmerald = TextPrimary
val AnchorEmeraldDim = TextSecondary
val AnchorAmber = UrgencyCoral
val AnchorRose = UrgencyCoral
val AnchorTextPrimary = TextPrimary
val AnchorTextSecondary = TextSecondary
val AnchorTextTertiary = TextTertiary
val AnchorOutline = HairlineColor
val AnchorOutlineVariant = HairlineColor
