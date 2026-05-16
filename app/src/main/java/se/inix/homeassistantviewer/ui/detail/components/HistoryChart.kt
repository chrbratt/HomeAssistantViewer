package se.inix.homeassistantviewer.ui.detail.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
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
 * Renders [series] as a zoomable, pannable line chart using Vico.
 *
 * Behavioural notes:
 *  - Numeric series use a smooth cubic point connector with an area fill.
 *  - Binary series use [LineCartesianLayer.PointConnector.Sharp] so the 0/1
 *    transitions look like step changes rather than diagonal interpolations.
 *  - Points with `value == null` (e.g. "unavailable" sensor readings) are
 *    dropped so the line does not fake-bridge through them.
 *
 * **X-axis representation:** Vico's layout math passes X values through
 * `Float` at draw time, and a raw epoch-second value (~1.7×10⁹) sits at the
 * edge of `Float` precision — points within ~128 seconds of each other can
 * collapse onto the same X coordinate. We therefore feed the chart
 * **relative seconds** (0 .. range) and reconstruct the absolute timestamp
 * inside the bottom-axis formatter using the captured series start.
 *
 * **Default zoom:** [Zoom.Content] fits the entire data into the viewport
 * by default — without this, Vico starts at its native point spacing and
 * the user has to pinch-zoom out to see the data.
 *
 * **Colors:** all components pull from [MaterialTheme] so the chart adapts
 * cleanly to light/dark themes.
 *
 * **Y labels inside:** the start axis is positioned with
 * [VerticalAxis.HorizontalLabelPosition.Inside] so the labels overlay the
 * chart area instead of taking horizontal space — gives the line more room
 * on narrow phones.
 */
@Composable
internal fun HistoryChart(
    series: HistorySeries,
    range: HistoryRange,
    modifier: Modifier = Modifier
) {
    val plottable = remember(series) { series.points.filter { it.value != null } }
    val xOffsetSeconds = remember(plottable) {
        plottable.firstOrNull()?.timestamp?.epochSecond ?: 0L
    }
    val xs = remember(plottable, xOffsetSeconds) {
        plottable.map { (it.timestamp.epochSecond - xOffsetSeconds).toDouble() }
    }
    val ys = remember(plottable) { plottable.mapNotNull { it.value } }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(xs, ys) {
        if (xs.size >= 2) {
            modelProducer.runTransaction { lineSeries { series(xs, ys) } }
        }
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val areaColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val labelColor = MaterialTheme.colorScheme.onSurface
    val guidelineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val axisLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    val pointConnector = remember(series.kind) {
        when (series.kind) {
            is SeriesKind.Binary -> LineCartesianLayer.PointConnector.Sharp
            else -> LineCartesianLayer.PointConnector.cubic()
        }
    }

    val bottomFormatter = remember(range, xOffsetSeconds) {
        rangeTimeFormatter(range, xOffsetSeconds)
    }

    val labelComponent = rememberAxisLabelComponent(color = labelColor)
    val guidelineComponent = rememberAxisGuidelineComponent(fill = fill(guidelineColor))
    val axisLineComponent = rememberAxisLineComponent(fill = fill(axisLineColor))

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(fill(lineColor)),
                        areaFill = if (series.kind is SeriesKind.Numeric) {
                            LineCartesianLayer.AreaFill.single(fill(areaColor))
                        } else {
                            null
                        },
                        pointConnector = pointConnector
                    )
                )
            ),
            startAxis = VerticalAxis.rememberStart(
                label = labelComponent,
                horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Inside,
                guideline = guidelineComponent,
                line = axisLineComponent
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                label = labelComponent,
                valueFormatter = bottomFormatter,
                guideline = guidelineComponent,
                line = axisLineComponent
            )
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),
        scrollState = rememberVicoScrollState(),
        zoomState = rememberVicoZoomState(
            zoomEnabled = true,
            initialZoom = Zoom.Content
        )
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
