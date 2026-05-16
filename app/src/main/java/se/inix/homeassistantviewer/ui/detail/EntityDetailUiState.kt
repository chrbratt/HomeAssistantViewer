package se.inix.homeassistantviewer.ui.detail

import se.inix.homeassistantviewer.data.model.HaEntityState
import se.inix.homeassistantviewer.domain.history.HistorySeries

/**
 * Render states for the entity detail screen. Distinct from
 * [DashboardUiState] because the detail flow has different concerns
 * (per-range loading, empty history vs no entity, etc.).
 */
sealed class EntityDetailUiState {
    data object Loading : EntityDetailUiState()

    /**
     * Series loaded successfully. [currentState] is the latest known entity
     * state — kept here so the top-of-screen value is always in sync with
     * what the chart's right edge represents.
     */
    data class Loaded(
        val series: HistorySeries,
        val currentState: HaEntityState?
    ) : EntityDetailUiState()

    /** HA's recorder has no rows for the requested window. */
    data class Empty(val currentState: HaEntityState?) : EntityDetailUiState()

    data class Error(val message: String) : EntityDetailUiState()
}
