package se.inix.homeassistantviewer.ui.detail.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.patrykandpatrick.vico.compose.cartesian.VicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.VicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.common.data.MutableExtraStore
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

internal data class ChartFrame(
    val xs: List<Double>,
    val ys: List<Double>,
    val xOffsetSeconds: Long
)

internal fun buildChartFrame(
    xs: List<Double>,
    ys: List<Double>,
    xOffsetSeconds: Long
): ChartFrame? {
    if (xs.size < 2 || ys.size < 2) return null
    return ChartFrame(xs, ys, xOffsetSeconds)
}

internal data class ChartPlotPoints(
    val minX: Double,
    val maxX: Double,
    val points: List<Pair<Double, Double>>
)

internal data class AdaptiveYRange(
    val minY: Double,
    val maxY: Double
)

internal val ChartPlotPointsKey = ExtraStore.Key<ChartPlotPoints>()
internal val AdaptiveYRangeKey = ExtraStore.Key<AdaptiveYRange>()

internal object AdaptiveVisibleYRangeProvider : CartesianLayerRangeProvider {
    private val fallback = CartesianLayerRangeProvider.auto()

    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        extraStore.getOrNull(AdaptiveYRangeKey)?.minY
            ?: fallback.getMinY(minY, maxY, extraStore)

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        extraStore.getOrNull(AdaptiveYRangeKey)?.maxY
            ?: fallback.getMaxY(minY, maxY, extraStore)
}

@Composable
internal fun rememberAdaptiveYValueFormatter(): CartesianValueFormatter =
    remember {
        CartesianValueFormatter { context, value, verticalAxisPosition ->
            val span = context.ranges.getYRange(verticalAxisPosition).length
            formatChartAxisValue(value, span)
        }
    }

@Composable
internal fun rememberAdaptiveStartAxis(
    label: com.patrykandpatrick.vico.compose.common.component.TextComponent,
    guideline: com.patrykandpatrick.vico.compose.common.component.LineComponent,
    axisLine: com.patrykandpatrick.vico.compose.common.component.LineComponent,
    valueFormatter: CartesianValueFormatter = rememberAdaptiveYValueFormatter()
): VerticalAxis<Axis.Position.Vertical.Start> =
    VerticalAxis.rememberStart(
        label = label,
        horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Inside,
        guideline = guideline,
        line = axisLine,
        valueFormatter = valueFormatter,
        // Fixed "nice" Y step (1/2/5 ×10ⁿ) derived from the currently visible
        // range, so labels land on round values and keep a stable spacing
        // while zooming instead of arbitrary auto-placed numbers.
        itemPlacer = remember {
            VerticalAxis.ItemPlacer.step(step = { store ->
                store.getOrNull(AdaptiveYRangeKey)?.let { range ->
                    niceStep((range.maxY - range.minY) / AdaptiveYTargetTicks)
                        .takeIf { it > 0.0 }
                }
            })
        }
    )

private const val AdaptiveYTargetTicks = 5.0

@Composable
internal fun SyncAdaptiveYRange(
    plotPoints: ChartPlotPoints?,
    modelProducer: CartesianChartModelProducer,
    scrollState: VicoScrollState,
    zoomState: VicoZoomState,
    publishSeries: CartesianChartModelProducer.Transaction.() -> Unit
) {
    var baselineZoom by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(plotPoints, scrollState, zoomState, modelProducer) {
        if (plotPoints == null || plotPoints.points.size < 2) return@LaunchedEffect
        snapshotFlow {
            Triple(scrollState.value, scrollState.maxValue, zoomState.value)
        }.collect { (scroll, maxScroll, zoom) ->
            if (baselineZoom <= 0f && zoom > 0f) {
                baselineZoom = zoom
            }
            val adaptiveRange = computeAdaptiveYRange(
                plotPoints = plotPoints,
                scroll = scroll,
                maxScroll = maxScroll,
                baselineZoom = baselineZoom.takeIf { it > 0f } ?: zoom,
                currentZoom = zoom
            )
            modelProducer.runTransaction {
                extras { store: MutableExtraStore ->
                    store[ChartPlotPointsKey] = plotPoints
                    store[AdaptiveYRangeKey] = adaptiveRange
                }
                publishSeries()
            }
        }
    }
}

internal fun ChartFrame.toPlotPoints(): ChartPlotPoints {
    val points = xs.zip(ys)
    return ChartPlotPoints(
        minX = xs.minOrNull() ?: 0.0,
        maxX = xs.maxOrNull() ?: 0.0,
        points = points
    )
}

internal fun mergePlotPoints(frames: List<ChartFrame>): ChartPlotPoints? {
    if (frames.isEmpty()) return null
    val points = frames.flatMap { frame -> frame.xs.zip(frame.ys) }
    if (points.size < 2) return null
    return ChartPlotPoints(
        minX = frames.minOf { it.xs.minOrNull() ?: 0.0 },
        maxX = frames.maxOf { it.xs.maxOrNull() ?: 0.0 },
        points = points
    )
}

internal fun computeVisibleXRange(
    minX: Double,
    maxX: Double,
    scroll: Float,
    maxScroll: Float,
    baselineZoom: Float,
    currentZoom: Float
): ClosedFloatingPointRange<Double> {
    val fullSpan = (maxX - minX).coerceAtLeast(1e-9)
    val zoomFraction = if (currentZoom > 0f) {
        (baselineZoom / currentZoom).coerceIn(0.001f, 1.0f).toDouble()
    } else {
        1.0
    }
    val scrollFraction = if (maxScroll > 0f) {
        (scroll / maxScroll).coerceIn(0f, 1f).toDouble()
    } else {
        0.0
    }
    val visibleSpan = fullSpan * zoomFraction
    val maxStart = maxX - visibleSpan
    val start = minX + scrollFraction * (maxStart - minX).coerceAtLeast(0.0)
    return start..(start + visibleSpan)
}

internal fun computeAdaptiveYRange(
    plotPoints: ChartPlotPoints,
    scroll: Float,
    maxScroll: Float,
    baselineZoom: Float,
    currentZoom: Float
): AdaptiveYRange {
    val visibleX = computeVisibleXRange(
        minX = plotPoints.minX,
        maxX = plotPoints.maxX,
        scroll = scroll,
        maxScroll = maxScroll,
        baselineZoom = baselineZoom,
        currentZoom = currentZoom
    )
    val visibleYs = plotPoints.points
        .asSequence()
        .filter { (x, _) -> x in visibleX }
        .map { (_, y) -> y }
        .toList()
    val ys = visibleYs.ifEmpty { plotPoints.points.map { it.second } }
    if (ys.isEmpty()) return AdaptiveYRange(0.0, 1.0)
    if (ys.size == 1) {
        val value = ys.first()
        val pad = max(kotlin.math.abs(value) * 0.05, 0.5)
        return AdaptiveYRange(value - pad, value + pad)
    }
    val rawMin = ys.min()
    val rawMax = ys.max()
    val span = (rawMax - rawMin).coerceAtLeast(1e-9)
    val padding = span * 0.08
    return AdaptiveYRange(
        minY = roundDown(rawMin - padding, span),
        maxY = roundUp(rawMax + padding, span)
    )
}

internal fun formatChartAxisValue(value: Double, span: Double): String {
    val decimals = when {
        span >= 100 -> 0
        span >= 10 -> 1
        span >= 1 -> 1
        span >= 0.1 -> 2
        span >= 0.01 -> 3
        else -> 4
    }
    return formatFixedDecimals(value, decimals)
}

/** Decimal places used when rendering a value inside a touch marker/tooltip. */
internal const val MARKER_VALUE_DECIMALS: Int = 2

internal fun formatFixedDecimals(value: Double, decimals: Int): String {
    if (decimals <= 0) return kotlin.math.round(value).toLong().toString()
    val factor = 10.0.pow(decimals)
    val rounded = kotlin.math.round(value * factor) / factor
    val text = rounded.toString()
    if (!text.contains('.')) return text
    return text.trimEnd('0').trimEnd('.')
}

private fun roundUp(value: Double, span: Double): Double {
    val step = niceStep(span / 5.0)
    return ceil(value / step) * step
}

private fun roundDown(value: Double, span: Double): Double {
    val step = niceStep(span / 5.0)
    return floor(value / step) * step
}

private fun niceStep(rawStep: Double): Double {
    if (rawStep <= 0.0 || rawStep.isNaN()) return 1.0
    val magnitude = 10.0.pow(floor(log10(rawStep)))
    val normalized = rawStep / magnitude
    val nice = when {
        normalized <= 1.0 -> 1.0
        normalized <= 2.0 -> 2.0
        normalized <= 5.0 -> 5.0
        else -> 10.0
    }
    return nice * magnitude
}
