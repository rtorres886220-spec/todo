package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Design language: "Refined Minimalist Light with High Contrast"
// Architectural simplicity: crisp obsidian typography (#0F172A), gallery white surfaces (#FAFAFA / #FFFFFF),
// hairline slate borders (#E2E8F0), and an authoritative cobalt blue accent (#2563EB).

val SoftLightBackground = Color(0xFFFAFAFA)
val SoftLightRadialLift = Color(0xFFFFFFFF)

// Backward-compatibility aliases for existing imports
val SoftDarkBackground = SoftLightBackground
val SoftDarkRadialLift = SoftLightRadialLift

// Surface tokens: Pure crisp white (Surface1) & light slate container (Surface2)
val Surface1 = Color(0xFFFFFFFF)
val Surface2 = Color(0xFFF1F5F9)
val SurfaceSunken = Color(0xFFE2E8F0)

// Clean structural boundaries and natural subtle drop shadows
val SurfaceHighlight = Color(0x00FFFFFF) // Deprecated fake 3D bevels
val HairlineColor = Color(0xFFE2E8F0)
val ShadowAmbient = Color(0x0A0F172A)
val ShadowOcclusion = Color(0x080F172A)

// Text tokens: primary deep obsidian onyx #0F172A, secondary slate #475569, tertiary mist #94A3B8
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val TextTertiary = Color(0xFF94A3B8)

// Primary accent: Crisp cobalt blue #2563EB (commanding, ultra-clean contrast)
val AccentGold = Color(0xFF2563EB)
val AccentGoldDim = Color(0x182563EB)
val AccentGoldGlow = Color(0x102563EB)
val GoldOnDarkText = Color(0xFFFFFFFF)

// Urgency: Ember crimson #DC2626 (for overdue tasks)
val UrgencyCoral = Color(0xFFDC2626)
val UrgencyCoralDim = Color(0x1ADC2626)

// Category tints: clean, high-contrast, modern pastel pills
val TintOrchidStart = Color(0xFFFAF5FF)
val TintOrchidEnd = Color(0xFFF3E8FF)
val TintOrchidFg = Color(0xFF7E22CE)

val TintSageStart = Color(0xFFF0FDF4)
val TintSageEnd = Color(0xFFDCFCE7)
val TintSageFg = Color(0xFF15803D)

val TintApricotStart = Color(0xFFFFFBEB)
val TintApricotEnd = Color(0xFFFEF3C7)
val TintApricotFg = Color(0xFFB45309)

val TintSlateStart = Color(0xFFF8FAFC)
val TintSlateEnd = Color(0xFFF1F5F9)
val TintSlateFg = Color(0xFF1E293B)

val TintRoseStart = Color(0xFFFFF1F2)
val TintRoseEnd = Color(0xFFFFE4E6)
val TintRoseFg = Color(0xFFBE123C)

// Swipe wash colors in light mode
val SwipeWashToday = Color(0x1E2563EB)
val SwipeWashSnooze = Color(0x1C64748B)
val SwipeWashDone = Color(0x1E15803D)

