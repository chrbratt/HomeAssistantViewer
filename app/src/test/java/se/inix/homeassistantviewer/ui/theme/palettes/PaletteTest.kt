package se.inix.homeassistantviewer.ui.theme.palettes

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.inix.homeassistantviewer.data.settings.ColorPalette

/**
 * Lightweight invariants for the hand-crafted palettes. We don't test
 * the dynamic palette — it's API-level dependent and resolved at runtime
 * by Material You — but every hand-crafted palette must:
 *
 *  1. Keep status colours visually distinct so the dashboard banner
 *     remains readable (primaryContainer != errorContainer, and
 *     tertiaryContainer is distinct from both).
 *  2. Use different surface colours between Light and Dark variants —
 *     otherwise "Dark mode" would look identical to "Light mode" for
 *     that palette, defeating the whole point.
 *  3. Cover the [ColorPalette] enum so adding a new value to the enum
 *     without a matching palette object surfaces immediately as a
 *     missing test case.
 */
class PaletteTest {

    private val handCrafted: List<Pair<ColorPalette, AppPalette>> = listOf(
        ColorPalette.OCEAN  to OceanPalette,
        ColorPalette.AURORA to AuroraPalette,
        ColorPalette.SUNSET to SunsetPalette,
    )

    @Test
    fun `every non-dynamic palette has a defined object`() {
        val covered = handCrafted.map { it.first }.toSet()
        val expected = ColorPalette.entries.filter { it != ColorPalette.DYNAMIC }.toSet()
        assertTrue(
            "Missing palette object for ${expected - covered}",
            covered.containsAll(expected)
        )
    }

    @Test
    fun `status colours stay distinguishable in every dark scheme`() {
        handCrafted.forEach { (id, p) ->
            assertStatusColoursDistinct(p.dark, label = "$id dark")
        }
    }

    @Test
    fun `status colours stay distinguishable in every light scheme`() {
        handCrafted.forEach { (id, p) ->
            assertStatusColoursDistinct(p.light, label = "$id light")
        }
    }

    @Test
    fun `light and dark variants differ for every palette`() {
        handCrafted.forEach { (id, p) ->
            assertNotEquals(
                "$id: light and dark must not have identical surface",
                p.light.surface, p.dark.surface
            )
            assertNotEquals(
                "$id: light and dark must not have identical primary",
                p.light.primary, p.dark.primary
            )
        }
    }

    @Test
    fun `palettes are not accidentally aliased to the same scheme`() {
        // Catches "OceanPalette : AppPalette by AuroraPalette" style mistakes.
        val darkPrimaries = handCrafted.map { it.first to it.second.dark.primary }
        val uniquePrimaries = darkPrimaries.map { it.second }.toSet()
        assertTrue(
            "Palettes share dark.primary: $darkPrimaries",
            uniquePrimaries.size == darkPrimaries.size
        )
    }

    private fun assertStatusColoursDistinct(scheme: ColorScheme, label: String) {
        val healthy = scheme.primaryContainer
        val warning = scheme.tertiaryContainer
        val error = scheme.errorContainer

        assertColoursDifferent(
            "$label: healthy and error containers must differ",
            healthy, error
        )
        assertColoursDifferent(
            "$label: healthy and warning containers must differ",
            healthy, warning
        )
        assertColoursDifferent(
            "$label: warning and error containers must differ",
            warning, error
        )
    }

    /**
     * Asserts two colours aren't *visually* identical. We use a loose
     * channel-wise diff (>= 0.08 on at least one of R/G/B in 0..1 space)
     * so palettes that accidentally pick near-identical hex values for two
     * "different" roles are flagged before they reach the user.
     */
    private fun assertColoursDifferent(message: String, a: Color, b: Color) {
        val drMaxDiff = maxOf(
            kotlin.math.abs(a.red - b.red),
            kotlin.math.abs(a.green - b.green),
            kotlin.math.abs(a.blue - b.blue),
        )
        assertTrue("$message (diff=$drMaxDiff, a=$a, b=$b)", drMaxDiff >= 0.08f)
    }
}
