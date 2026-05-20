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
        assertTrue("tightSpacing must shrink (${c.tightSpacing} → ${k.tightSpacing})", k.tightSpacing < c.tightSpacing)
        assertTrue("iconBoxSize must shrink (${c.iconBoxSize} → ${k.iconBoxSize})", k.iconBoxSize < c.iconBoxSize)
        assertTrue("iconSize must shrink (${c.iconSize} → ${k.iconSize})", k.iconSize < c.iconSize)
        assertTrue("gridContentPadding must shrink (${c.gridContentPadding} → ${k.gridContentPadding})",
            k.gridContentPadding < c.gridContentPadding)
        assertTrue("gridItemSpacing must shrink (${c.gridItemSpacing} → ${k.gridItemSpacing})",
            k.gridItemSpacing < c.gridItemSpacing)
        assertTrue("sliderHeight must shrink (${c.sliderHeight} → ${k.sliderHeight})", k.sliderHeight < c.sliderHeight)
        assertTrue("switchScale must shrink (${c.switchScale} → ${k.switchScale})", k.switchScale < c.switchScale)
        assertTrue("switchRowHeight must shrink (${c.switchRowHeight} → ${k.switchRowHeight})", k.switchRowHeight < c.switchRowHeight)
        assertTrue("actionIconButtonSize must shrink (${c.actionIconButtonSize} → ${k.actionIconButtonSize})",
            k.actionIconButtonSize < c.actionIconButtonSize)
        assertTrue("actionIconSize must shrink (${c.actionIconSize} → ${k.actionIconSize})", k.actionIconSize < c.actionIconSize)
        assertTrue("actionGap must shrink (${c.actionGap} → ${k.actionGap})", k.actionGap < c.actionGap)
        assertTrue("controlIconButtonSize must shrink (${c.controlIconButtonSize} → ${k.controlIconButtonSize})",
            k.controlIconButtonSize < c.controlIconButtonSize)
        assertTrue("controlIconSize must shrink (${c.controlIconSize} → ${k.controlIconSize})", k.controlIconSize < c.controlIconSize)
        assertTrue("primaryControlIconButtonSize must shrink (${c.primaryControlIconButtonSize} → ${k.primaryControlIconButtonSize})",
            k.primaryControlIconButtonSize < c.primaryControlIconButtonSize)
        assertTrue("primaryControlIconSize must shrink (${c.primaryControlIconSize} → ${k.primaryControlIconSize})",
            k.primaryControlIconSize < c.primaryControlIconSize)
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
