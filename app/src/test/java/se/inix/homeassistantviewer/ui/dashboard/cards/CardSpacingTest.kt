package se.inix.homeassistantviewer.ui.dashboard.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.inix.homeassistantviewer.data.settings.Density

/**
 * Guard-rail invariants for the density presets. The actual `dp` numbers
 * are free to drift as the design evolves — but two relationships must
 * hold for the feature to make sense to a user:
 *
 *  1. Every spacing dimension on [CardSpacing.Compact] is strictly
 *     smaller than its [CardSpacing.Comfortable] counterpart. Otherwise
 *     "Compact" wouldn't actually be more compact for that dimension.
 *  2. [CardSpacing.forDensity] covers the [Density] enum exhaustively;
 *     adding a new value to the enum without a matching preset should
 *     surface here, not as a `when` warning that's easy to miss.
 */
class CardSpacingTest {

    @Test
    fun `every dimension shrinks from Comfortable to Compact`() {
        val c = CardSpacing.Comfortable
        val k = CardSpacing.Compact
        assertTrue("padding must shrink (${c.padding} → ${k.padding})", k.padding < c.padding)
        assertTrue("spacing must shrink (${c.spacing} → ${k.spacing})", k.spacing < c.spacing)
        assertTrue("iconBoxSize must shrink (${c.iconBoxSize} → ${k.iconBoxSize})", k.iconBoxSize < c.iconBoxSize)
        assertTrue("iconSize must shrink (${c.iconSize} → ${k.iconSize})", k.iconSize < c.iconSize)
        assertTrue("gridContentPadding must shrink (${c.gridContentPadding} → ${k.gridContentPadding})",
            k.gridContentPadding < c.gridContentPadding)
        assertTrue("gridItemSpacing must shrink (${c.gridItemSpacing} → ${k.gridItemSpacing})",
            k.gridItemSpacing < c.gridItemSpacing)
    }

    @Test
    fun `forDensity maps every enum value to a distinct preset`() {
        val mapped = Density.entries.map { CardSpacing.forDensity(it) }
        assertEquals("every Density must map to a preset", Density.entries.size, mapped.size)
        assertEquals("presets must be distinct", mapped.toSet().size, mapped.size)
    }

    @Test
    fun `forDensity is total`() {
        // Compiles → exhaustiveness is guaranteed by the `when` in the
        // production code. This test just asserts no exception leaks for
        // any enum value at runtime.
        Density.entries.forEach { CardSpacing.forDensity(it) }
    }
}
