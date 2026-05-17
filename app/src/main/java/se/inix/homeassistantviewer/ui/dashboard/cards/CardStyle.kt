package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.data.settings.Density

/**
 * Visual spacing tokens used by every dashboard card and the grid that
 * holds them. Two presets are exposed (see [Comfortable] / [Compact])
 * and bound at the top of the composition by `MainActivity` via
 * [LocalCardSpacing].
 *
 * Existing call sites use [CardStyle] which delegates to the current
 * [LocalCardSpacing] value — that way switching density only requires
 * the user to flip a setting; no card knows about the enum at all.
 */
internal data class CardSpacing(
    val padding: Dp,
    val spacing: Dp,
    val iconBoxSize: Dp,
    val iconSize: Dp,
    val gridContentPadding: Dp,
    val gridItemSpacing: Dp,
) {
    companion object {
        /**
         * Numbers chosen for a ~30% vertical-footprint reduction vs the
         * original Material default-ish 16/10/40/22 sizing, while
         * keeping touch targets ≥ 36 dp (M3 adds its own
         * `minimumInteractiveComponentSize` on switches/icon-buttons).
         */
        val Comfortable = CardSpacing(
            padding = 12.dp,
            spacing = 6.dp,
            iconBoxSize = 32.dp,
            iconSize = 18.dp,
            gridContentPadding = 8.dp,
            gridItemSpacing = 8.dp,
        )

        /**
         * Tighter spacing for users who want more cards visible at once.
         * Padding/spacing shrink by ~33%; the icon badge shrinks slightly
         * so it doesn't visually dominate the smaller card. Grid spacing
         * halves so cards almost butt up against each other.
         */
        val Compact = CardSpacing(
            padding = 8.dp,
            spacing = 4.dp,
            iconBoxSize = 26.dp,
            iconSize = 14.dp,
            gridContentPadding = 4.dp,
            gridItemSpacing = 4.dp,
        )

        fun forDensity(density: Density): CardSpacing = when (density) {
            Density.COMFORTABLE -> Comfortable
            Density.COMPACT -> Compact
        }
    }
}

/**
 * The active [CardSpacing] for this branch of the composition tree.
 * Defaults to [CardSpacing.Comfortable] so previews and lower-level
 * tests don't need to wire the provider explicitly.
 */
internal val LocalCardSpacing = compositionLocalOf { CardSpacing.Comfortable }

/**
 * Backward-compatible accessor used by every card. Reads
 * [LocalCardSpacing] at composition time, so call sites stay unchanged
 * even though the values are now dynamic.
 */
internal object CardStyle {
    val Padding: Dp
        @Composable get() = LocalCardSpacing.current.padding
    val Spacing: Dp
        @Composable get() = LocalCardSpacing.current.spacing
    val IconBoxSize: Dp
        @Composable get() = LocalCardSpacing.current.iconBoxSize
    val IconSize: Dp
        @Composable get() = LocalCardSpacing.current.iconSize
}
