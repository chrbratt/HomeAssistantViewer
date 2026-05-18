package se.inix.homeassistantviewer.ui.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.domain.history.HistoryRange

/**
 * Single row of filter chips for the chart's time window.
 *
 * Separated from [EntityDetailScreen] so the chip row can be previewed and
 * reused independently — and so the screen layout file stays under 100
 * lines.
 */
@Composable
internal fun TimeRangeChips(
    selected: HistoryRange,
    onSelect: (HistoryRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HistoryRange.entries.forEach { range ->
            FilterChip(
                selected = range == selected,
                onClick = { onSelect(range) },
                label = {
                    Text(text = range.label, style = MaterialTheme.typography.labelMedium)
                }
            )
        }
    }
}
