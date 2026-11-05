package com.rentease.app.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Brand follows the website (teal primary, coral accent, warm background) so the app feels like the same product.
internal val LightColors = lightColorScheme(
    primary = WebPrimary,
    onPrimary = WebOnPrimary,
    primaryContainer = WebSecondary,
    onPrimaryContainer = WebOnSecondary,
    secondary = WebAccentStrong,
    onSecondary = Color.White,
    secondaryContainer = WebAccentSoft,
    onSecondaryContainer = WebForeground,
    tertiary = WebPrimaryGlow,
    background = WebBackground,
    onBackground = WebForeground,
    surface = Color.White,
    onSurface = WebForeground,
    surfaceVariant = WebMuted,
    onSurfaceVariant = WebMutedForeground,
    outline = WebOutline,
    outlineVariant = WebBorder,
    error = RentEaseError,
    onError = Color.White,
    errorContainer = WebErrorContainer,
    onErrorContainer = WebOnErrorContainer,
    onTertiary = WebOnTertiary,
    tertiaryContainer = WebSecondary,
    onTertiaryContainer = WebOnSecondary,
    surfaceDim = WebSurfaceDim,
    surfaceBright = Color.White,
    surfaceContainerLowest = WebSurfaceLowest,
    surfaceContainerLow = WebSurfaceLow,
    surfaceContainer = WebSurfaceContainer,
    surfaceContainerHigh = WebSurfaceHigh,
    surfaceContainerHighest = WebSurfaceHighest,
    inverseSurface = WebInverseSurface,
    inverseOnSurface = WebInverseOnSurface,
    inversePrimary = WebDarkPrimary,
    scrim = Color.Black
)

internal val DarkColors = darkColorScheme(
    primary = WebDarkPrimary,
    onPrimary = WebDarkOnPrimary,
    primaryContainer = WebDarkSecondary,
    onPrimaryContainer = WebDarkForeground,
    secondary = WebDarkAccent,
    onSecondary = WebDarkOnAccent,
    secondaryContainer = WebDarkMuted,
    onSecondaryContainer = WebDarkForeground,
    tertiary = WebDarkPrimaryGlow,
    background = WebDarkBackground,
    onBackground = WebDarkForeground,
    surface = WebDarkSurface,
    onSurface = WebDarkForeground,
    surfaceVariant = WebDarkMuted,
    onSurfaceVariant = WebDarkMutedForeground,
    outline = WebDarkOutlineStrong,
    outlineVariant = WebDarkOutline,
    error = WebDarkError,
    onError = WebDarkOnError,
    errorContainer = WebDarkErrorContainer,
    onErrorContainer = WebDarkOnErrorContainer,
    onTertiary = WebDarkOnPrimary,
    tertiaryContainer = WebDarkSecondary,
    onTertiaryContainer = WebDarkForeground,
    surfaceDim = WebDarkBackground,
    surfaceBright = WebDarkSurfaceBright,
    surfaceContainerLowest = WebDarkSurfaceLowest,
    surfaceContainerLow = WebDarkSurfaceLow,
    surfaceContainer = WebDarkSurfaceContainer,
    surfaceContainerHigh = WebDarkSurfaceHigh,
    surfaceContainerHighest = WebDarkSurfaceHighest,
    inverseSurface = WebDarkInverseSurface,
    inverseOnSurface = WebDarkInverseOnSurface,
    inversePrimary = WebPrimary,
    scrim = Color.Black
)

/** Rounder corners, like the website's rounded-3xl cards. */
private val RentEaseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun RentEaseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color (Material You) is intentionally OFF by default: RentEase has a
    // defined navy/gold brand identity (Part 1 §2.1/Phase 11) that should not be
    // overridden by the user's wallpaper-derived palette.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RentEaseTypography,
        shapes = RentEaseShapes,
        content = content
    )
}

private typealias Color = androidx.compose.ui.graphics.Color
