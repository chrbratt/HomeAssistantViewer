package se.inix.homeassistantviewer.ui.detail.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import se.inix.homeassistantviewer.domain.history.HistoryRange
import se.inix.homeassistantviewer.domain.history.HistorySeries
import se.inix.homeassistantviewer.domain.history.SeriesKind
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Entry point for rendering an entity's [HistorySeries].
 *
 * Numeric and binary series render very differently:
 *  - Numeric (temperature, lux, power, …) → smooth cubic line chart with
 *    free Y range, drawn via Vico's `LineCartesianLayer`.
 *  - Binary (switch, light, lock, binary_sensor) → HA-style state timeline
 *    with filled ON blocks, drawn via Compose Canvas. Vico's line layer
 *    can't natively produce flat-top step shapes — see
 *    [BinaryStateTimeline] for the rationale.
 *
 * Dispatching here keeps each chart focused on its own visualization
 * problem rather than overloading one composable with conditionals.
 */
@Composable
internal fun HistoryChart(
    series: HistorySeries,
    range: HistoryRange,
    modifier: Modifier = Modifier
) {
    when (series.kind) {
        is SeriesKind.Binary -> BinaryStateTimeline(series, range, modifier)
        else -> NumericHistoryChart(series, range, modifier)
    }
}

/**
 * Smooth cubic line chart for continuous numeric series (temperature,
 * humidity, lux, power, etc.). Y axis auto-fits the data, Y labels live
 * inside the chart area so the line gets the full width.
 */
@Composable
private fun NumericHistoryChart(
    series: HistorySeries,
    range: HistoryRange,
    modifier: Modifier = Modifier
) {
    val frame = remember(series) { buildFrame(series) } ?: return

    val lineColor = MaterialTheme.colorScheme.primary
    val areaColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val components = rememberAxisComponents()
    val bottomFormatter = remember(range, frame.xOffsetSeconds) {
        rangeTimeFormatter(range, frame.xOffsetSeconds)
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(frame) {
        modelProducer.runTransaction { lineSeries { series(frame.xs, frame.ys) } }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(fill(lineColor)),
                        areaFill = LineCartesianLayer.AreaFill.single(fill(areaColor)),
                        pointConnector = LineCartesianLayer.PointConnector.cubic()
                    )
                )
            ),
            startAxis = VerticalAxis.rememberStart(
                label = components.label,
                horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Inside,
                guideline = components.guideline,
                line = components.axisLine
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                label = components.label,
                valueFormatter = bottomFormatter,
                guideline = components.guideline,
                line = components.axisLine
            )
        ),
        modelProducer = modelProducer,
        // Sizing comes from the caller; the detail screen stretches the
        // chart to fill the available space in portrait and switches to a
        // side-by-side layout in landscape.
        modifier = modifier,
        scrollState = rememberVicoScrollState(),
        zoomState = rememberVicoZoomState(
            zoomEnabled = true,
            initialZoom = Zoom.Content
        )
    )
}

/**
 * Pre-projected X/Y arrays the Vico chart needs.
 *
 * **X-axis representation:** Vico's layout math passes X values through
 * `Float` at draw time, and a raw epoch-second value (~1.7×10⁹) sits at
 * the edge of `Float` precision — points within ~128 seconds of each
 * other can collapse onto the same X coordinate. We therefore feed the
 * chart **relative seconds** (0 .. range) and reconstruct the absolute
 * timestamp inside the bottom-axis formatter using [xOffsetSeconds].
 */
private data class ChartFrame(
    val xs: List<Double>,
    val ys: List<Double>,
    val xOffsetSeconds: Long
)

private fun buildFrame(series: HistorySeries): ChartFrame? {
    val plottable = series.points.filter { it.value != null }
    if (plottable.size < 2) return null
    val offset = plottable.first().timestamp.epochSecond
    val xs = plottable.map { (it.timestamp.epochSecond - offset).toDouble() }
    val ys = plottable.mapNotNull { it.value }
    return ChartFrame(xs, ys, offset)
}

/**
 * Theme-aware axis components for the numeric chart. Centralising the
 * styling means a future "label-component overhaul" only happens in one
 * place.
 */
private data class AxisComponents(
    val label: com.patrykandpatrick.vico.core.common.component.TextComponent,
    val guideline: com.patrykandpatrick.vico.core.common.component.LineComponent,
    val axisLine: com.patrykandpatrick.vico.core.common.component.LineComponent
)

@Composable
private fun rememberAxisComponents(): AxisComponents {
    val labelColor = MaterialTheme.colorScheme.onSurface
    val guidelineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val axisLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    return AxisComponents(
        label = rememberAxisLabelComponent(color = labelColor),
        guideline = rememberAxisGuidelineComponent(fill = fill(guidelineColor)),
        axisLine = rememberAxisLineComponent(fill = fill(axisLineColor))
    )
}

/**
 * Picks a label format that gives enough resolution at the selected range:
 *  - Hour     -> "HH:mm:ss"
 *  - Day      -> "HH:mm"
 *  - Week     -> "EEE HH:mm" (weekday + time)
 *
 * Built once per range and used for every label call so the chart does not
 * re-allocate `DateTimeFormatter` instances on every frame.
 *
 * The [offsetSeconds] is the epoch second of the first plotted point; chart
 * x-values are relative to this, so the formatter adds it back before
 * rendering a wall-clock time.
 */
private fun rangeTimeFormatter(
    range: HistoryRange,
    offsetSeconds: Long
): CartesianValueFormatter {
    val pattern = when (range) {
        HistoryRange.Hour -> "HH:mm:ss"
        HistoryRange.Day -> "HH:mm"
        HistoryRange.Week -> "EEE HH:mm"
    }
    val formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault())
    return CartesianValueFormatter { _, value, _ ->
        formatter.format(Instant.ofEpochSecond(offsetSeconds + value.toLong()))
    }
}
