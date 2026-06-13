package se.inix.homeassistantviewer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import se.inix.homeassistantviewer.data.settings.TextContrast
import se.inix.homeassistantviewer.ui.theme.palettes.AmberPalette
import se.inix.homeassistantviewer.ui.theme.palettes.AppPalette
import se.inix.homeassistantviewer.ui.theme.palettes.AuroraPalette
import se.inix.homeassistantviewer.ui.theme.palettes.CitrinePalette
import se.inix.homeassistantviewer.ui.theme.palettes.EmberPalette
import se.inix.homeassistantviewer.ui.theme.palettes.OceanPalette
import se.inix.homeassistantviewer.ui.theme.palettes.SunsetPalette

/**
 * Guards the readability of body text after the two scheme transforms
 * applied at runtime ([withTonalSurfaces] + [withTextContrast]).
 *
 * The softening feature deliberately lowers text contrast for comfort, so
 * the invariant that matters is: *even at the softest level*, body text on
 * every surface — including the most elevated card — still clears WCAG AA
 * (4.5:1 for body, 3:1 for the lower-emphasis variant). If a future tweak
 * to the softening amounts or palettes drops below that, this fails first.
 */
class ContrastTest {

    private val palettes: List<Pair<String, AppPalette>> = listOf(
        "Ocean" to OceanPalette,
        "Aurora" to AuroraPalette,
        "Sunset" to SunsetPalette,
        "Ember" to EmberPalette,
        "Amber" to AmberPalette,
        "Citrine" to CitrinePalette,
    )

    @Test
    fun `body text stays AA-readable at the softest contrast level`() {
        val failures = mutableListOf<String>()
        palettes.forEach { (id, p) ->
            check(id, "light", p.light.withTonalSurfaces(dark = false), failures)
            check(id, "dark", p.dark.withTonalSurfaces(dark = true), failures)
        }
        assertTrue(
            "Contrast below WCAG AA:\n${failures.joinToString("\n")}",
            failures.isEmpty()
        )
    }

    private fun check(id: String, mode: String, base: ColorScheme, failures: MutableList<String>) {
        val s = base.withTextContrast(TextContrast.SOFT)
        // Primary body text on the page and on the most elevated card.
        requireRatio("$id $mode onSurface/surface", s.onSurface, s.surface, 4.5, failures)
        requireRatio("$id $mode onBackground/background", s.onBackground, s.background, 4.5, failures)
        requireRatio(
            "$id $mode onSurface/surfaceContainerHighest",
            s.onSurface, s.surfaceContainerHighest, 4.5, failures
        )
        // Lower-emphasis secondary text: large/secondary threshold.
        requireRatio(
            "$id $mode onSurfaceVariant/surface",
            s.onSurfaceVariant, s.surface, 3.0, failures
        )
    }

    private fun requireRatio(
        label: String,
        fg: Color,
        bg: Color,
        min: Double,
        failures: MutableList<String>
    ) {
        val ratio = contrastRatio(fg, bg)
        if (ratio < min) {
            failures += "$label = %.2f:1 (needs %.1f:1)".format(ratio, min)
        }
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** WCAG 2.1 relative luminance from sRGB channels. */
    private fun relativeLuminance(c: Color): Double {
        fun lin(channel: Float): Double {
            val v = channel.toDouble()
            return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * lin(c.red) + 0.7152 * lin(c.green) + 0.0722 * lin(c.blue)
    }
}
