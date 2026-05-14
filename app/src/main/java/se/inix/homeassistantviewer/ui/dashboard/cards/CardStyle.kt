package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.ui.unit.dp

/**
 * Shared visual dimensions for every dashboard card. Defined once so a future
 * "even more compact" or "spacious" toggle is a one-line change.
 *
 * Numbers chosen for a ~30% vertical-footprint reduction vs the original
 * Material default-ish 16/10/40/22 sizing while keeping touch targets ≥ 36 dp.
 */
internal object CardStyle {
    val Padding = 12.dp
    val Spacing = 6.dp
    val IconBoxSize = 32.dp
    val IconSize = 18.dp
}
