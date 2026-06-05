package se.inix.homeassistantviewer.ui.dashboard.cards

/**
 * When non-null, the card is in comparison selection mode: the checkbox is
 * visible and the title row accepts long-press to toggle selection.
 */
data class ComparisonSelectionUi(
    val isSelected: Boolean,
    val onToggle: () -> Unit
)
