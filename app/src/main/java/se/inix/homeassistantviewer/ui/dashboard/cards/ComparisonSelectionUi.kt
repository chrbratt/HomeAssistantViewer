package se.inix.homeassistantviewer.ui.dashboard.cards

/**
 * Comparison-selection wiring for a dashboard card.
 *
 * Passed for every comparable card so a long-press on the card title can
 * *start* selection. [selectionModeActive] controls whether the selection
 * checkbox is actually shown (true once at least one entity is selected).
 */
data class ComparisonSelectionUi(
    val isSelected: Boolean,
    val selectionModeActive: Boolean,
    val onToggle: () -> Unit
)
