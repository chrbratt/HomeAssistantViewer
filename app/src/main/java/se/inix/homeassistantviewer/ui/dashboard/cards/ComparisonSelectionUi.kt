package se.inix.homeassistantviewer.ui.dashboard.cards

/**
 * When non-null, the card can be marked for comparison via long-press.
 * Only [isSelected] cards show a check indicator and selection outline —
 * unselected cards stay visually unchanged (no empty checkboxes everywhere).
 */
data class ComparisonSelectionUi(
    val isSelected: Boolean,
    val onToggle: () -> Unit
)
