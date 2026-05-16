package se.inix.homeassistantviewer.domain.history

import java.time.Instant

/**
 * One observation in the entity history timeline. Always has a timestamp
 * (HA stamps every row with `last_changed`); the [value] is null for
 * unparseable / "unavailable" states which the chart renders as a gap.
 *
 * For binary entities the chart projects [value] = 0 (off) or 1 (on)
 * before plotting, so a single point type works for both numeric and
 * binary series.
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

    /** Catch-all for state strings with no useful numeric mapping. */
    data object Discrete : SeriesKind()
}

/**
 * Bundle returned to the UI: the points plus the [kind] decides which
 * chart renderer to use.
 */
data class HistorySeries(
    val points: List<HistoryPoint>,
    val kind: SeriesKind
)
