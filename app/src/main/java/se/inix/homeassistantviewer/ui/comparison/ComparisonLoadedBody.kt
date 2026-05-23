package se.inix.homeassistantviewer.ui.comparison

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.domain.comparison.ComparisonExclusionReason
import se.inix.homeassistantviewer.domain.history.HistoryRange
import se.inix.homeassistantviewer.ui.comparison.components.ComparisonBinaryTimelineSection
import se.inix.homeassistantviewer.ui.comparison.components.ComparisonCategoricalTimelineSection
import se.inix.homeassistantviewer.ui.comparison.components.ComparisonChartSection
import se.inix.homeassistantviewer.ui.detail.components.TimeRangeChips

@Composable
internal fun ComparisonLoadedBody(
    state: ComparisonUiState.Loaded,
    selectedRange: HistoryRange,
    onSelectRange: (HistoryRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasContent = state.chartGroups.isNotEmpty() ||
        state.binarySeries.isNotEmpty() ||
        state.categoricalSeries.isNotEmpty()
    val sectionCount = state.chartGroups.size +
        (if (state.binarySeries.isNotEmpty()) 1 else 0) +
        (if (state.categoricalSeries.isNotEmpty()) 1 else 0)
    val singleNumericOnly = sectionCount == 1 && state.chartGroups.size == 1

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TimeRangeChips(
            selected = selectedRange,
            onSelect = onSelectRange
        )

        when {
            !hasContent -> Text(
                text = "No history available for the selected entities in this range.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            singleNumericOnly -> ComparisonChartSection(
                group = state.chartGroups.first(),
                range = state.range,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                chartModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                state.chartGroups.forEach { group ->
                    ComparisonChartSection(
                        group = group,
                        range = state.range,
                        modifier = Modifier.fillMaxWidth(),
                        chartModifier = Modifier
                            .fillMaxWidth()
                            .height(ComparisonChartHeight)
                    )
                }
                if (state.binarySeries.isNotEmpty()) {
                    ComparisonBinaryTimelineSection(
                        series = state.binarySeries,
                        range = state.range,
                        modifier = Modifier.fillMaxWidth(),
                        chartModifier = Modifier.fillMaxWidth()
                    )
                }
                if (state.categoricalSeries.isNotEmpty()) {
                    ComparisonCategoricalTimelineSection(
                        series = state.categoricalSeries,
                        range = state.range,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (state.excluded.isNotEmpty()) {
            ComparisonExcludedList(state = state)
        }
    }
}

@Composable
private fun ComparisonExcludedList(state: ComparisonUiState.Loaded) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Not shown in graph",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        state.excluded.forEach { entry ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = entry.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = when (entry.reason) {
                            ComparisonExclusionReason.NO_DATA ->
                                "No plottable history in the selected range."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Fixed height per chart when several sections are stacked in a scroll view. */
private val ComparisonChartHeight = 300.dp
