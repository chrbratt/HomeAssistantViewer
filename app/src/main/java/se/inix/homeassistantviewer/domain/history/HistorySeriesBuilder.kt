package se.inix.homeassistantviewer.domain.history

import se.inix.homeassistantviewer.data.model.HaHistoryRow
import java.time.Instant

/**
 * Glue that converts raw [HaHistoryRow]s from the REST API into a
 * typed [HistorySeries] the chart can render. Pure function — no I/O,
 * no Android types — so it can be unit-tested in isolation.
 *
 * The [domain] is the HA entity domain (e.g. "switch", "sensor") and
 * decides the initial [SeriesKind]; [unitOfMeasurement] feeds Numeric series.
 */
object HistorySeriesBuilder {

    /** Max points to feed into the chart. Keeps Vico responsive on pan/zoom. */
    private const val TARGET_POINTS = 400

    /** Domains that always use the categorical timeline, never binary upgrade. */
    private val forceCategoricalDomains = setOf("input_select", "select")

    fun build(
        rows: List<HaHistoryRow>,
        domain: String,
        unitOfMeasurement: String?
    ): HistorySeries {
        val initialKind = SeriesClassifier.classify(domain, unitOfMeasurement)
        val points = rows
            .mapNotNull { row ->
                val lastChanged = row.lastChanged ?: return@mapNotNull null
                val ts = runCatching { Instant.parse(lastChanged) }.getOrNull()
                    ?: return@mapNotNull null
                val rawState = row.state
                val projected = rawState?.let { SeriesClassifier.project(initialKind, it) }
                HistoryPoint(
                    timestamp = ts,
                    value = projected,
                    rawState = rawState.orEmpty()
                )
            }
            .sortedBy { it.timestamp }

        val kind = finalizeKind(initialKind, points, domain)
        val projectedPoints = when (kind) {
            is SeriesKind.Binary if initialKind !is SeriesKind.Binary ->
                points.map { p ->
                    p.copy(value = SeriesClassifier.project(SeriesKind.Binary, p.rawState))
                }
            is SeriesKind.Numeric if initialKind !is SeriesKind.Numeric ->
                points.map { p ->
                    p.copy(value = SeriesClassifier.project(kind, p.rawState))
                }
            else -> points
        }

        val downsampled = when (kind) {
            is SeriesKind.Categorical ->
                Downsampler.downsampleCategorical(projectedPoints, TARGET_POINTS)
            else ->
                Downsampler.downsample(projectedPoints, TARGET_POINTS)
        }
        return HistorySeries(points = downsampled, kind = kind)
    }

    private fun finalizeKind(
        initialKind: SeriesKind,
        points: List<HistoryPoint>,
        domain: String
    ): SeriesKind {
        if (initialKind is SeriesKind.Binary || initialKind is SeriesKind.Numeric) {
            return initialKind
        }

        if (domain in forceCategoricalDomains) {
            return SeriesKind.Categorical(states = orderedDistinctStates(points))
        }

        if (domain == "sensor") {
            val plottable = points.filter { isPlottableHistoryState(it.rawState) }
            if (plottable.isNotEmpty() &&
                plottable.all { it.rawState.toDoubleOrNull() != null }
            ) {
                return SeriesKind.Numeric(unit = null)
            }
        }

        val plottable = points.filter { isPlottableHistoryState(it.rawState) }
        if (plottable.isEmpty()) {
            return SeriesKind.Categorical(states = emptyList())
        }

        val distinctLower = plottable.map { it.rawState.lowercase() }.toSet()
        val allMapToBinary = distinctLower.all {
            SeriesClassifier.project(SeriesKind.Binary, it) != null
        }
        if (distinctLower.size <= 2 && allMapToBinary) {
            return SeriesKind.Binary
        }
        return SeriesKind.Categorical(states = orderedDistinctStates(points))
    }
}
