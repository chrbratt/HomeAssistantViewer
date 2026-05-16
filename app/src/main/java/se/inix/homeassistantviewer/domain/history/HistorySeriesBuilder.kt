package se.inix.homeassistantviewer.domain.history

import se.inix.homeassistantviewer.data.model.HaHistoryRow
import java.time.Instant

/**
 * Glue that converts raw [HaHistoryRow]s from the REST API into a
 * typed [HistorySeries] the chart can render. Pure function — no I/O,
 * no Android types — so it can be unit-tested in isolation.
 *
 * The [domain] is the HA entity domain (e.g. "switch", "sensor") and
 * decides the [SeriesKind]; [unitOfMeasurement] feeds Numeric series.
 */
internal object HistorySeriesBuilder {

    /** Max points to feed into the chart. Keeps Vico responsive on pan/zoom. */
    private const val TARGET_POINTS = 400

    fun build(
        rows: List<HaHistoryRow>,
        domain: String,
        unitOfMeasurement: String?
    ): HistorySeries {
        val kind = SeriesClassifier.classify(domain, unitOfMeasurement)
        val points = rows
            // A row needs a parseable timestamp to be plotted at all — that
            // is the X coordinate. Rows missing/malformed `last_changed` are
            // dropped. A null `state` is kept as a gap (value = null,
            // rawState = ""), which the chart renders as a break in the
            // line instead of fake-bridging through unknown data.
            .mapNotNull { row ->
                val lastChanged = row.lastChanged ?: return@mapNotNull null
                val ts = runCatching { Instant.parse(lastChanged) }.getOrNull()
                    ?: return@mapNotNull null
                val rawState = row.state
                val projected = rawState?.let { SeriesClassifier.project(kind, it) }
                HistoryPoint(
                    timestamp = ts,
                    value = projected,
                    rawState = rawState.orEmpty()
                )
            }
            .sortedBy { it.timestamp }

        val downsampled = Downsampler.downsample(points, TARGET_POINTS)
        return HistorySeries(points = downsampled, kind = kind)
    }
}
