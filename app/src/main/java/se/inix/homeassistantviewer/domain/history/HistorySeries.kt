package se.inix.homeassistantviewer.domain.history

import java.time.Instant

/**
 * One observation in the entity history timeline. Always has a timestamp
 * (HA stamps every row with `last_changed`); the [value] is null for
 * unparseable / "unavailable" states which the chart renders as a gap.
 *
 * For binary entities the chart projects [value] = 0 (off) or 1 (on)
 * before plotting, so a single point type works for both numeric and
 * binary series. Categorical entities keep [value] null and rely on
 * [rawState] for the state timeline renderer.
 */
data class HistoryPoint(
    val timestamp: Instant,
    val value: Double?,
    val rawState: String
)

/**
 * Distinguishes how a series should be rendered. Computed once when the
 * series is built, so the UI never has to re-classify.
 */
sealed class SeriesKind {
    /** Sensor-style series with a continuous numeric value and unit. */
    data class Numeric(val unit: String?) : SeriesKind()

    /** Switch-style series — only two states matter (on/off, locked/unlocked). */
    data object Binary : SeriesKind()

    /**
     * Named text states (weather, select, device_tracker, text sensors, …).
     * [states] lists distinct values in first-seen order for legend colours.
     */
    data class Categorical(val states: List<String>) : SeriesKind()
}

/**
 * Bundle returned to the UI: the points plus the [kind] decides which
 * chart renderer to use.
 */
data class HistorySeries(
    val points: List<HistoryPoint>,
    val kind: SeriesKind
) {
    /** Whether the series has enough data for its renderer to draw meaningfully. */
    fun hasPlottableData(): Boolean = when (kind) {
        is SeriesKind.Binary, is SeriesKind.Numeric ->
            points.count { it.value != null } >= 2
        is SeriesKind.Categorical ->
            points.count { isPlottableHistoryState(it.rawState) } >= 2
    }
}
