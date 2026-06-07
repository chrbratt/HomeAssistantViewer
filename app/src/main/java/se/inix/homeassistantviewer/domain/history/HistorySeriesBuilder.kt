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

    /**
     * Upper bound on points fed to the chart. Sized so full-resolution
     * statistics survive without downsampling — the densest case is a
     * 6-month window of hourly long-term statistics (~4 320 points) — while
     * still capping pathological raw-state series so Vico stays responsive
     * on pan/zoom.
     */
    private const val TARGET_POINTS = 5000

    /** Domains that always use the categorical timeline, never binary upgrade. */
    private val forceCategoricalDomains = setOf("input_select", "select")

    fun build(
        rows: List<HaHistoryRow>,
        domain: String,
        unitOfMeasurement: String?
    ): HistorySeries {
        val full = buildFull(rows, domain, unitOfMeasurement)
        val downsampled = when (full.kind) {
            is SeriesKind.Categorical ->
                Downsampler.downsampleCategorical(full.points, TARGET_POINTS)
            else ->
                Downsampler.downsample(full.points, TARGET_POINTS)
        }
        return full.copy(points = downsampled)
    }

    /** Full-resolution series for CSV export — no downsampling. */
    fun buildFull(
        rows: List<HaHistoryRow>,
        domain: String,
        unitOfMeasurement: String?
    ): HistorySeries {
        val initialKind = SeriesClassifier.classify(domain, unitOfMeasurement)
        val points = parsePoints(rows, initialKind)
        val kind = finalizeKind(initialKind, points, domain)
        val projectedPoints = projectPoints(points, initialKind, kind)
        return HistorySeries(points = projectedPoints, kind = kind)
    }

    private fun parsePoints(
        rows: List<HaHistoryRow>,
        initialKind: SeriesKind
    ): List<HistoryPoint> =
        rows
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

    private fun projectPoints(
        points: List<HistoryPoint>,
        initialKind: SeriesKind,
        kind: SeriesKind
    ): List<HistoryPoint> = when {
        kind is SeriesKind.Binary && initialKind !is SeriesKind.Binary ->
            points.map { p ->
                p.copy(value = SeriesClassifier.project(SeriesKind.Binary, p.rawState))
            }
        kind is SeriesKind.Numeric && initialKind !is SeriesKind.Numeric ->
            points.map { p ->
                p.copy(value = SeriesClassifier.project(kind, p.rawState))
            }
        else -> points
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
