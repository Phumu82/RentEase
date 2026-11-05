package com.rentease.app.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Guards both themes (light and dark) against unreadable text: every text/background pair the app's
 * components actually produce must reach WCAG AA (4.5:1), and outlines/icons 3:1.
 */
class ThemeContrastTest {

    private fun lum(c: Color): Double {
        fun ch(v: Int): Double { val x = v / 255.0; return if (x <= 0.03928) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4) }
        val argb = c.toArgb()
        return 0.2126 * ch((argb shr 16) and 0xFF) + 0.7152 * ch((argb shr 8) and 0xFF) + 0.0722 * ch(argb and 0xFF)
    }

    private fun ratio(a: Color, b: Color): Double {
        val l1 = lum(a); val l2 = lum(b)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun check(name: String, scheme: ColorScheme) {
        val backgrounds = mapOf(
            "background" to scheme.background,
            "surface" to scheme.surface,
            "surfaceContainer" to scheme.surfaceContainer,
            "surfaceContainerHigh" to scheme.surfaceContainerHigh,
            "surfaceContainerHighest" to scheme.surfaceContainerHighest
        )
        val failures = mutableListOf<String>()
        for ((bgName, bg) in backgrounds) {
            mapOf(
                "onSurface" to scheme.onSurface,
                "onSurfaceVariant" to scheme.onSurfaceVariant,
                "primary" to scheme.primary,
                "secondary" to scheme.secondary,
                "error" to scheme.error
            ).forEach { (fgName, fg) ->
                if (ratio(fg, bg) < 4.5) failures += "$name: $fgName on $bgName = %.2f".format(ratio(fg, bg))
            }
            if (ratio(scheme.outline, bg) < 3.0) failures += "$name: outline on $bgName = %.2f".format(ratio(scheme.outline, bg))
        }
        val filled = listOf(
            "onPrimary/primary" to (scheme.onPrimary to scheme.primary),
            "onSecondary/secondary" to (scheme.onSecondary to scheme.secondary),
            "onError/error" to (scheme.onError to scheme.error),
            "onErrorContainer/errorContainer" to (scheme.onErrorContainer to scheme.errorContainer),
            "onPrimaryContainer/primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
            "onSecondaryContainer/secondaryContainer" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
            "inverseOnSurface/inverseSurface" to (scheme.inverseOnSurface to scheme.inverseSurface)
        )
        for ((label, pair) in filled) {
            if (ratio(pair.first, pair.second) < 4.5) failures += "$name: $label = %.2f".format(ratio(pair.first, pair.second))
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test fun `light theme text is readable on every surface`() = check("light", LightColors)

    @Test fun `dark theme text is readable on every surface`() = check("dark", DarkColors)

    @Test
    fun `dark surfaces are actually dark and the ladder steps lighter`() {
        val steps = listOf(
            DarkColors.surfaceContainerLowest, DarkColors.surfaceContainerLow, DarkColors.surfaceContainer,
            DarkColors.surfaceContainerHigh, DarkColors.surfaceContainerHighest
        ).map(::lum)
        assertTrue(steps.zipWithNext().all { (a, b) -> a < b })
        assertTrue("dark background must be dark", lum(DarkColors.background) < 0.02)
        assertTrue("dark cards must be dark", lum(DarkColors.surfaceContainerHighest) < 0.06)
    }
}
