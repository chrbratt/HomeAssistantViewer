package se.inix.homeassistantviewer.ui.comparison.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import se.inix.homeassistantviewer.domain.comparison.ComparisonChartGroup
import se.inix.homeassistantviewer.domain.comparison.ComparisonSeriesEntry
import se.inix.homeassistantviewer.domain.history.HistoryPoint
import se.inix.homeassistantviewer.domain.history.HistoryRange
import se.inix.homeassistantviewer.domain.history.HistorySeries
import se.inix.homeassistantviewer.ui.detail.components.AdaptiveVisibleYRangeProvider
import se.inix.homeassistantviewer.ui.detail.components.ChartFrame
import se.inix.homeassistantviewer.ui.detail.components.SyncAdaptiveYRange
import se.inix.homeassistantviewer.ui.detail.components.buildChartFrame
import se.inix.homeassistantviewer.ui.detail.components.mergePlotPoints
import se.inix.homeassistantviewer.ui.detail.components.rememberAdaptiveStartAxis
import se.inix.homeassistantviewer.ui.detail.components.rememberZeroLineDecoration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun ColumnScope.ComparisonChartSection(
    group: ComparisonChartGroup,
    range: HistoryRange,
    modifier: Modifier = Modifier,
    chartModifier: Modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
) {
    var hiddenKeys by remember { mutableStateOf(emptySet<String>()) }
    val colors = comparisonSeriesColors(group.series.size)
    val visibleSeries = group.series.filter { it.legendKey() !in hiddenKeys }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = group.unitLabel,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        ComparisonLegend(
            series = group.series,
            colors = colors,
            hiddenKeys = hiddenKeys,
            onToggle = { entry ->
                val key = entry.legendKey()
                hiddenKeys = if (key in hiddenKeys) hiddenKeys - key else hiddenKeys + key
            }
        )
        MultiNumericHistoryChart(
            series = visibleSeries,
            range = range,
            colors = colors,
            modifier = chartModifier
        )
    }
}

@Composable
private fun MultiNumericHistoryChart(
    series: List<ComparisonSeriesEntry>,
    range: HistoryRange,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val nowEpoch by produceState(initialValue = Instant.now().epochSecond) {
        while (true) {
            delay(30_000L)
            value = Instant.now().epochSecond
        }
    }
    val xOffsetSeconds = range.startEpoch(nowEpoch)
    val frames = remember(series, xOffsetSeconds) {
        series.mapNotNull { entry ->
            buildFrame(entry.series, xOffsetSeconds)?.let { entry to it }
        }
    }
    if (frames.isEmpty()) return

    val plotPoints = remember(frames) { mergePlotPoints(frames.map { it.second }) }
    val components = rememberAxisComponents()
    val bottomFormatter = remember(range, xOffsetSeconds) {
        rangeTimeFormatter(range, xOffsetSeconds)
    }

    val scrollState = rememberVicoScrollState()
    val zoomState = rememberVicoZoomState(
        zoomEnabled = true,
        initialZoom = Zoom.Content
    )

    val lines = frames.map { (entry, _) ->
        val color = colors.getOrElse(entry.colorIndex) { colors.first() }
        LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(Fill(color)),
            areaFill = null,
            interpolator = LineCartesianLayer.Interpolator.cubic()
        )
    }

    val includesNegative = remember(frames) {
        frames.any { (_, frame) -> frame.ys.any { it < 0.0 } }
    }
    val zeroLine = rememberZeroLineDecoration(includesNegative)
    val decorations = remember(zeroLine) { listOfNotNull(zeroLine) }

    val modelProducer = remember { CartesianChartModelProducer() }

    SyncAdaptiveYRange(
        plotPoints = plotPoints,
        modelProducer = modelProducer,
        scrollState = scrollState,
        zoomState = zoomState
    ) {
        lineModel {
            frames.forEach { (_, frame) ->
                series(frame.xs, frame.ys)
            }
        }
    }

    val chart = rememberCartesianChart(
        rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(lines),
            rangeProvider = remember { AdaptiveVisibleYRangeProvider }
        ),
        startAxis = rememberAdaptiveStartAxis(
            label = components.label,
            guideline = components.guideline,
            axisLine = components.axisLine
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            label = components.label,
            valueFormatter = bottomFormatter,
            guideline = components.guideline,
            line = components.axisLine
        ),
        marker = rememberComparisonMarker(frames, xOffsetSeconds, range),
        decorations = decorations
    )

    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        modifier = modifier,
        scrollState = scrollState,
        zoomState = zoomState
    )
}

private fun buildFrame(series: HistorySeries, xOffsetSeconds: Long): ChartFrame? {
    val plottable = series.points.filter { it.value != null }
    if (plottable.size < 2) return null
    val xs = plottable.map { (it.timestamp.epochSecond - xOffsetSeconds).toDouble() }
    val ys = plottable.mapNotNull(HistoryPoint::value)
    return buildChartFrame(xs, ys, xOffsetSeconds)
}

private data class AxisComponents(
    val label: com.patrykandpatrick.vico.compose.common.component.TextComponent,
    val guideline: com.patrykandpatrick.vico.compose.common.component.LineComponent,
    val axisLine: com.patrykandpatrick.vico.compose.common.component.LineComponent
)

@Composable
private fun rememberAxisComponents(): AxisComponents {
    val labelColor = MaterialTheme.colorScheme.onSurface
    val guidelineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val axisLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val labelStyle = MaterialTheme.typography.bodySmall.copy(color = labelColor)
    return AxisComponents(
        label = rememberAxisLabelComponent(style = labelStyle),
        guideline = rememberAxisGuidelineComponent(fill = Fill(guidelineColor)),
        axisLine = rememberAxisLineComponent(fill = Fill(axisLineColor))
    )
}

private fun rangeTimeFormatter(
    range: HistoryRange,
    offsetSeconds: Long
): CartesianValueFormatter {
    val pattern = range.axisTimePattern
    val formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault())
    return CartesianValueFormatter { _, value, _ ->
        formatter.format(Instant.ofEpochSecond(offsetSeconds + value.toLong()))
    }
}

@Composable
private fun rememberComparisonMarker(
    frames: List<Pair<ComparisonSeriesEntry, ChartFrame>>,
    xOffsetSeconds: Long,
    range: HistoryRange
): DefaultCartesianMarker {
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurface
    )
    val timePattern = range.markerTimePattern
    val timeFormatter = remember(range) {
        DateTimeFormatter.ofPattern(timePattern).withZone(ZoneId.systemDefault())
    }
    return rememberDefaultCartesianMarker(
        label = rememberTextComponent(style = labelStyle),
        valueFormatter = remember(frames, xOffsetSeconds, timeFormatter) {
            DefaultCartesianMarker.ValueFormatter { _, targets ->
                val lineTargets = targets.filterIsInstance<LineCartesianLayerMarkerTarget>()
                if (lineTargets.isEmpty()) return@ValueFormatter ""
                val x = lineTargets.first().x
                val timestamp = timeFormatter.format(
                    Instant.ofEpochSecond(xOffsetSeconds + x.toLong())
                )
                buildString {
                    appendLine(timestamp)
                    lineTargets.forEachIndexed { seriesIndex, target ->
                        target.points.forEach { point ->
                            val meta = frames.getOrNull(seriesIndex)?.first
                            val name = meta?.displayName ?: "Series ${seriesIndex + 1}"
                            val unitSuffix = meta?.unit?.let { " $it" }.orEmpty()
                            appendLine("$name: ${point.entry.y}$unitSuffix")
                        }
                    }
                }.trim()
            }
        }
    )
}
