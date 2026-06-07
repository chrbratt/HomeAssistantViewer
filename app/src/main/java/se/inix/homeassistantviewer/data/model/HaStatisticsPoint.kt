package se.inix.homeassistantviewer.data.model

import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * One aggregated bucket from Home Assistant's
 * `recorder/statistics_during_period` WebSocket command.
 *
 * @property startMs bucket start, epoch milliseconds (HA returns UTC ms).
 * @property mean    bucket mean; `null` when the bucket has no value.
 */
data class HaStatisticsPoint(
    val startMs: Long,
    val mean: Double?
)

/**
 * Adapts statistics buckets to the same [HaHistoryRow] shape the chart
 * pipeline already consumes, so statistics and raw states share one code
 * path downstream. Pure (no I/O / Android types) for easy unit testing.
 */
object HaStatisticsConverter {

    private val formatter = DateTimeFormatter.ISO_INSTANT

    fun toHistoryRows(points: List<HaStatisticsPoint>): List<HaHistoryRow> =
        points.mapNotNull { point ->
            val mean = point.mean ?: return@mapNotNull null
            HaHistoryRow(
                state = formatStateValue(mean),
                lastChanged = formatter.format(Instant.ofEpochMilli(point.startMs))
            )
        }

    /** Drops a trailing ".0" so integers read cleanly, keeps real decimals. */
    private fun formatStateValue(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
