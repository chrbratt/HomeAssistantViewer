package se.inix.homeassistantviewer.ui.comparison.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.domain.comparison.ComparisonSeriesEntry

internal fun ComparisonSeriesEntry.legendKey(): String = "$connectionId:$entityId"

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ComparisonLegend(
    series: List<ComparisonSeriesEntry>,
    colors: List<Color>,
    hiddenKeys: Set<String>,
    onToggle: (ComparisonSeriesEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        series.forEach { entry ->
            val visible = entry.legendKey() !in hiddenKeys
            val color = colors.getOrElse(entry.colorIndex) { colors.first() }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onToggle(entry) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (visible) color else color.copy(alpha = 0.25f))
                )
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (visible) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    },
                    textDecoration = if (visible) TextDecoration.None else TextDecoration.LineThrough
                )
            }
        }
    }
}

@Composable
internal fun comparisonSeriesColors(count: Int): List<Color> {
    val scheme = MaterialTheme.colorScheme
    val palette = listOf(
        scheme.primary,
        scheme.secondary,
        scheme.tertiary,
        scheme.error,
        scheme.primaryContainer,
        scheme.secondaryContainer
    )
    return List(count) { index -> palette[index % palette.size] }
}
