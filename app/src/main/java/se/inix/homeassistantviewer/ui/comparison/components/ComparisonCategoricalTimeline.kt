package se.inix.homeassistantviewer.ui.comparison.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.domain.comparison.ComparisonSeriesEntry
import se.inix.homeassistantviewer.domain.history.HistoryRange
import se.inix.homeassistantviewer.ui.detail.components.CategoricalStateTimeline

private val ComparisonCategoricalChartHeight = 180.dp

@Composable
internal fun ColumnScope.ComparisonCategoricalTimelineSection(
    series: List<ComparisonSeriesEntry>,
    range: HistoryRange,
    modifier: Modifier = Modifier
) {
    var hiddenKeys by remember { mutableStateOf(emptySet<String>()) }
    val colors = comparisonSeriesColors(series.size)
    val visibleSeries = series.filter { it.legendKey() !in hiddenKeys }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "States",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        ComparisonLegend(
            series = series,
            colors = colors,
            hiddenKeys = hiddenKeys,
            onToggle = { entry ->
                val key = entry.legendKey()
                hiddenKeys = if (key in hiddenKeys) hiddenKeys - key else hiddenKeys + key
            }
        )
        visibleSeries.forEach { entry ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp)
                )
                CategoricalStateTimeline(
                    series = entry.series,
                    range = range,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ComparisonCategoricalChartHeight)
                )
            }
        }
    }
}
