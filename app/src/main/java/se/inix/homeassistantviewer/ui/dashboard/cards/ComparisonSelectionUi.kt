package se.inix.homeassistantviewer.ui.dashboard.cards

/**
 * When non-null, the card supports comparison selection via the header
 * checkbox or long-press on the title row.
 */
data class ComparisonSelectionUi(
    val isSelected: Boolean,
    val onToggle: () -> Unit
)
