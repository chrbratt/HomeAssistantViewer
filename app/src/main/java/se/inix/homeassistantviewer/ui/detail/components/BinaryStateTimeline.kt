package se.inix.homeassistantviewer.ui.detail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import se.inix.homeassistantviewer.domain.history.HistoryPoint
import se.inix.homeassistantviewer.domain.history.HistoryRange
import se.inix.homeassistantviewer.domain.history.HistorySeries
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Home Assistant–style state timeline for binary entities (switch, light,
 * lock, binary_sensor, …).
 *
 * Instead of forcing on/off into a line chart with vertical edges, this
 * timeline renders each ON period as a flat-top filled rectangle — readable
 * at a glance and matches HA's own history UI. ON periods are filled
 * blocks; OFF is empty space; transitions happen exactly at the recorded
 * timestamps with no interpolation artefacts.
 */
@Composable
internal fun BinaryStateTimeline(
    series: HistorySeries,
    range: HistoryRange,
    modifier: Modifier = Modifier
) {
    val nowEpoch by produceState(initialValue = Instant.now().epochSecond) {
        while (true) {
            delay(30_000L)
            value = Instant.now().epochSecond
        }
    }

    val window = remember(series, nowEpoch) { computeWindow(series.points, nowEpoch) } ?: return
    val intervals = remember(series, window) {
        computeOnIntervals(series.points, windowEnd = window.endEpoch)
    }

    var viewport by remember(range) { mutableStateOf(window) }
    LaunchedEffect(window) {
        val unzoomed = viewport.startEpoch == window.startEpoch &&
            window.endEpoch - viewport.endEpoch <= TimelineResnapToleranceSeconds
        if (unzoomed) viewport = window
    }

    val onColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val guidelineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val mutedLabelStyle = MaterialTheme.typography.labelSmall
        .copy(color = mutedColor, textAlign = TextAlign.Center)

    val textMeasurer = rememberTextMeasurer()
    val timeFormatter by remember {
        derivedStateOf { timeFormatterFor(viewport.spanSeconds) }
    }

    Canvas(
        modifier = modifier
            .pointerInput(window) {
                detectTapGestures(onDoubleTap = { viewport = window })
            }
            .pointerInput(Unit) {
                val leftPx = BinaryChartLeftPad.toPx()
                val rightPx = TimelineChartRightPad.toPx()
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val chartWidth = (size.width - leftPx - rightPx).coerceAtLeast(1f)
                    val centroidFraction = ((centroid.x - leftPx) / chartWidth)
                        .coerceIn(0f, 1f)
                    val panFraction = pan.x / chartWidth
                    viewport = applyGesture(
                        current = viewport,
                        dataWindow = window,
                        centroidFraction = centroidFraction,
                        zoom = zoom,
                        panFraction = panFraction
                    )
                }
            }
    ) {
        drawBinaryStateTimeline(
            intervals = intervals,
            viewport = viewport,
            onColor = onColor,
            labelStyle = labelStyle,
            mutedLabelStyle = mutedLabelStyle,
            guidelineColor = guidelineColor,
            borderColor = borderColor,
            textMeasurer = textMeasurer,
            timeFormatter = timeFormatter
        )
    }
}

/**
 * Converts the projected series into ON intervals. Each consecutive pair
 * `(p[i], p[i+1])` holds `p[i].value` until `p[i+1]`. We emit an interval
 * for any pair where the held value is ON (projected ≥ 0.5).
 */
internal fun computeOnIntervals(
    points: List<HistoryPoint>,
    windowEnd: Long
): List<Pair<Long, Long>> {
    if (points.isEmpty()) return emptyList()
    val intervals = mutableListOf<Pair<Long, Long>>()
    for (i in 0 until points.size - 1) {
        val v = points[i].value
        if (v != null && v >= 0.5) {
            val s = points[i].timestamp.epochSecond
            val e = points[i + 1].timestamp.epochSecond
            if (e > s) intervals += s to e
        }
    }
    val last = points.last()
    val lastValue = last.value
    if (lastValue != null && lastValue >= 0.5) {
        val s = last.timestamp.epochSecond
        if (windowEnd > s) intervals += s to windowEnd
    }
    return intervals
}

private fun DrawScope.drawBinaryStateTimeline(
    intervals: List<Pair<Long, Long>>,
    viewport: TimelineWindow,
    onColor: Color,
    labelStyle: TextStyle,
    mutedLabelStyle: TextStyle,
    guidelineColor: Color,
    borderColor: Color,
    textMeasurer: TextMeasurer,
    timeFormatter: DateTimeFormatter
) {
    val leftPad = BinaryChartLeftPad.toPx()
    val rightPad = TimelineChartRightPad.toPx()
    val topPad = TimelineChartTopPad.toPx()
    val bottomPad = TimelineChartBottomPad.toPx()

    val chartLeft = leftPad
    val chartRight = size.width - rightPad
    val chartTop = topPad
    val chartBottom = size.height - bottomPad
    val chartHeight = chartBottom - chartTop
    val span = viewport.spanSeconds.toDouble()

    fun xToPixel(epoch: Long): Float {
        val frac = ((epoch - viewport.startEpoch).toDouble() / span).toFloat()
        return chartLeft + frac * (chartRight - chartLeft)
    }

    val blockColor = onColor.copy(alpha = 0.85f)
    for ((start, end) in intervals) {
        if (end <= viewport.startEpoch || start >= viewport.endEpoch) continue
        val xs = xToPixel(start.coerceAtLeast(viewport.startEpoch))
        val xe = xToPixel(end.coerceAtMost(viewport.endEpoch))
        if (xe > xs) {
            drawRect(
                color = blockColor,
                topLeft = Offset(xs, chartTop),
                size = Size(xe - xs, chartHeight)
            )
        }
    }

    val tickCount = 5
    val ticks = List(tickCount) { i ->
        viewport.startEpoch + ((i.toDouble() / (tickCount - 1)) * span).roundToInt()
    }
    for (t in ticks) {
        val x = xToPixel(t)
        drawLine(
            color = guidelineColor,
            start = Offset(x, chartTop),
            end = Offset(x, chartBottom),
            strokeWidth = 0.8f
        )
    }

    drawRect(
        color = borderColor,
        topLeft = Offset(chartLeft, chartTop),
        size = Size(chartRight - chartLeft, chartHeight),
        style = Stroke(width = 1f)
    )

    val onLayout = textMeasurer.measure("ON", labelStyle)
    val offLayout = textMeasurer.measure("OFF", labelStyle)
    drawText(
        textLayoutResult = onLayout,
        topLeft = Offset(2.dp.toPx(), chartTop - 2.dp.toPx())
    )
    drawText(
        textLayoutResult = offLayout,
        topLeft = Offset(2.dp.toPx(), chartBottom - offLayout.size.height + 2.dp.toPx())
    )

    for (t in ticks) {
        val text = timeFormatter.format(Instant.ofEpochSecond(t))
        val layout = textMeasurer.measure(text, mutedLabelStyle)
        val centerX = xToPixel(t)
        val left = (centerX - layout.size.width / 2f)
            .coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(left, chartBottom + 4.dp.toPx())
        )
    }
}

/** Extra left padding for ON/OFF axis labels on the binary chart. */
private val BinaryChartLeftPad = 36.dp
