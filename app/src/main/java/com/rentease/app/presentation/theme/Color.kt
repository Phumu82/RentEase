package com.rentease.app.presentation.theme

import androidx.compose.ui.graphics.Color

// Navy — primary brand color (app icon background, per Part 1 §2.1)
val RentEaseNavy = Color(0xFF1B2A4A)
val RentEaseNavyDark = Color(0xFF101A30)
val RentEaseNavyLight = Color(0xFF2E4270)

// Gold — accent color (app icon graduation-cap accent)
val RentEaseGold = Color(0xFFE8A93B)
val RentEaseGoldLight = Color(0xFFFCD989)

val RentEaseSurfaceLight = Color(0xFFFAFAFC)
val RentEaseSurfaceDark = Color(0xFF15192A)

val RentEaseError = Color(0xFFB3261E)
val RentEaseSuccess = Color(0xFF2E7D32)
val RentEaseWarning = Color(0xFFED6C02)

// ---- Website palette (src/styles.css tokens converted to sRGB): teal primary, coral accent, warm paper background ----
val WebPrimary = Color(0xFF007886)
val WebPrimaryGlow = Color(0xFF2AADAD)
val WebOnPrimary = Color(0xFFF3FCFD)
val WebSecondary = Color(0xFFE4F4F6)
val WebOnSecondary = Color(0xFF10323F)
val WebBackground = Color(0xFFFDFCF7)
val WebForeground = Color(0xFF0D222D)
val WebMuted = Color(0xFFEBF5F6)
val WebMutedForeground = Color(0xFF5C6C75)
val WebAccent = Color(0xFFE96F42)
val WebOnAccent = Color(0xFFFFFAF5)
/** Darker coral for text and filled buttons/badges: 5.4:1 against white (the bright coral is only 3:1). */
val WebAccentStrong = Color(0xFFB9441B)
val WebOutline = Color(0xFF758790)
val WebAccentSoft = Color(0xFFFFE9D9)
val WebBorder = Color(0xFFD4E0E4)

val WebDarkBackground = Color(0xFF031019)
val WebDarkSurface = Color(0xFF091C26)
val WebDarkForeground = Color(0xFFECF3F5)
val WebDarkPrimary = Color(0xFF3CBAB6)
val WebDarkOnPrimary = Color(0xFF02111A)
val WebDarkPrimaryGlow = Color(0xFF71D9D0)
val WebDarkSecondary = Color(0xFF162934)
val WebDarkMuted = Color(0xFF16262F)
val WebDarkMutedForeground = Color(0xFF97A8AF)
val WebDarkAccent = Color(0xFFF78955)
val WebDarkOnAccent = Color(0xFF200B04)
val WebDarkOutlineStrong = Color(0xFF6B8591)
val WebDarkOutline = Color(0x1FFFFFFF)

// ---- Surface ladder + inverse/error roles. Material 3 components (Card, dialogs, menus, nav bars, filled
// text fields, chips) read these; left unset they fall back to the default PURPLE-grey baseline, which
// clashes with the teal palette - worst in dark mode. ----
val WebSurfaceLowest = Color(0xFFFFFFFF)
val WebSurfaceLow = Color(0xFFF8F7F1)
val WebSurfaceContainer = Color(0xFFF4F6F3)
val WebSurfaceHigh = Color(0xFFF1F5F4)
val WebSurfaceHighest = Color(0xFFFFFFFF)
val WebSurfaceDim = Color(0xFFE9EBE6)
val WebInverseSurface = Color(0xFF12303F)
val WebInverseOnSurface = Color(0xFFECF3F5)
val WebErrorContainer = Color(0xFFFFDAD6)
val WebOnErrorContainer = Color(0xFF410002)
val WebOnTertiary = Color(0xFF02242A)

val WebDarkSurfaceLowest = Color(0xFF020B12)
val WebDarkSurfaceLow = Color(0xFF061721)
val WebDarkSurfaceContainer = Color(0xFF0A1F2A)
val WebDarkSurfaceHigh = Color(0xFF102732)
val WebDarkSurfaceHighest = Color(0xFF17313E)
val WebDarkSurfaceBright = Color(0xFF1B3644)
val WebDarkInverseSurface = Color(0xFFECF3F5)
val WebDarkInverseOnSurface = Color(0xFF0D222D)
val WebDarkError = Color(0xFFFFB4AB)
val WebDarkOnError = Color(0xFF4A0A06)
val WebDarkErrorContainer = Color(0xFF5C1A14)
val WebDarkOnErrorContainer = Color(0xFFFFDAD6)
