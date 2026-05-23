package se.inix.homeassistantviewer.domain.comparison

import se.inix.homeassistantviewer.domain.history.HistorySeries
import se.inix.homeassistantviewer.domain.history.SeriesKind

/**
 * One entity's history prepared for the comparison chart.
 */
data class ComparisonSeriesEntry(
    val connectionId: String,
    val entityId: String,
    val displayName: String,
    val series: HistorySeries,
    val unit: String?,
    val colorIndex: Int
)

/**
 * A group of numeric series that share the same unit and are drawn in one chart.
 */
data class ComparisonChartGroup(
    val unitLabel: String,
    val series: List<ComparisonSeriesEntry>
)

enum class ComparisonExclusionReason {
    NO_DATA
}

data class ComparisonExcludedEntry(
    val connectionId: String,
    val entityId: String,
    val displayName: String,
    val reason: ComparisonExclusionReason
)

/**
 * Splits plottable series by kind:
 *  - numeric → one chart per distinct unit (°C, %, lx, …)
 *  - binary → stacked on/off timelines
 *  - categorical → state timelines per entity
 */
object ComparisonChartGrouper {

    fun group(
        entries: List<ComparisonSeriesEntry>,
        excluded: List<ComparisonExcludedEntry> = emptyList()
    ): ComparisonGroupingResult {
        val numeric = entries.filter { it.series.kind is SeriesKind.Numeric && it.series.hasPlottableData() }
        val binary = entries.filter { it.series.kind is SeriesKind.Binary && it.series.hasPlottableData() }
        val categorical = entries.filter {
            it.series.kind is SeriesKind.Categorical && it.series.hasPlottableData()
        }
        val noDataExcluded = entries.filter { !it.series.hasPlottableData() }
            .map {
                ComparisonExcludedEntry(
                    connectionId = it.connectionId,
                    entityId = it.entityId,
                    displayName = it.displayName,
                    reason = ComparisonExclusionReason.NO_DATA
                )
            }

        val allExcluded = excluded + noDataExcluded
        val numericGroups = if (numeric.isEmpty()) {
            emptyList()
        } else {
            val units = numeric.map { it.unit.orEmpty() }.distinct()
            units.map { unit ->
                ComparisonChartGroup(
                    unitLabel = formatUnitLabel(unit),
                    series = numeric
                        .filter { it.unit.orEmpty() == unit }
                        .mapIndexed { index, entry -> entry.copy(colorIndex = index) }
                )
            }
        }

        return ComparisonGroupingResult(
            chartGroups = numericGroups,
            binarySeries = binary.mapIndexed { index, entry -> entry.copy(colorIndex = index) },
            categoricalSeries = categorical.mapIndexed { index, entry -> entry.copy(colorIndex = index) },
            excluded = allExcluded
        )
    }

    internal fun formatUnitLabel(unit: String): String =
        unit.takeIf { it.isNotBlank() } ?: "No unit"

    fun hasPlottableContent(result: ComparisonGroupingResult): Boolean =
        result.chartGroups.isNotEmpty() ||
            result.binarySeries.isNotEmpty() ||
            result.categoricalSeries.isNotEmpty()
}

data class ComparisonGroupingResult(
    val chartGroups: List<ComparisonChartGroup>,
    val binarySeries: List<ComparisonSeriesEntry>,
    val categoricalSeries: List<ComparisonSeriesEntry>,
    val excluded: List<ComparisonExcludedEntry>
)
