package se.inix.homeassistantviewer.ui.detail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import se.inix.homeassistantviewer.domain.history.HistoryPoint
import se.inix.homeassistantviewer.domain.history.HistoryRange
import se.inix.homeassistantviewer.domain.history.HistorySeries
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
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
 *
 * **Interaction:**
 *  - Pinch to zoom (anchors at the centroid — you zoom into what your
 *    fingers are pointing at, not the chart centre).
 *  - Single-finger drag to pan horizontally.
 *  - Double-tap to reset the zoom to the full data window.
 *  - Switching range (1h / 24h / 7d) also resets the zoom — explicit
 *    range changes always win over a stale zoomed viewport.
 *
 * Drawn with Compose Canvas + TextMeasurer because Vico's line layer
 * can't natively produce flat-top step shapes without artificial step
 * points. Custom drawing means exact HA visual parity with zero
 * workarounds.
 */
@Composable
internal fun BinaryStateTimeline(
    series: HistorySeries,
    range: HistoryRange,
    modifier: Modifier = Modifier
) {
    // Re-evaluate the right edge every 30 s so a still-ON switch grows in
    // real time. The 30 s cadence is a balance between visual freshness
    // and not running 1 recomposition per second on the detail screen.
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

    // Visible time window — pinch/pan/double-tap mutate this. Resets on
    // explicit range switch; auto-follows live updates only when the user
    // is unzoomed (otherwise we'd yank them out of their inspection view).
    var viewport by remember(range) { mutableStateOf(window) }
    LaunchedEffect(window) {
        val unzoomed = viewport.startEpoch == window.startEpoch &&
            window.endEpoch - viewport.endEpoch <= ResnapToleranceSeconds
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
            .fillMaxWidth()
            .height(140.dp)
            // Double-tap → restore the full data window. Sits before the
            // transform detector so quick double-taps win the race.
            .pointerInput(window) {
                detectTapGestures(onDoubleTap = { viewport = window })
            }
            // Single-finger drag pans; pinch zooms. The detail screen
            // doesn't scroll vertically, so we can claim all touch events
            // here without nested-scroll headaches.
            .pointerInput(Unit) {
                val leftPx = ChartLeftPad.toPx()
                val rightPx = ChartRightPad.toPx()
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

/** Visible time range for the timeline (epoch seconds). */
internal data class TimelineWindow(val startEpoch: Long, val endEpoch: Long) {
    val spanSeconds: Long get() = (endEpoch - startEpoch).coerceAtLeast(1L)
}

/**
 * Picks the chart window. We pin the left edge at the first known data
 * point and the right edge at max(last data, now) — that way:
 *  - A switch that toggled once an hour ago and is still ON shows a wide
 *    ON block from that toggle up to now (instead of a zero-width dot).
 *  - A historical-only view (no live updates) still covers the data span.
 */
internal fun computeWindow(points: List<HistoryPoint>, nowEpoch: Long): TimelineWindow? {
    val first = points.firstOrNull()?.timestamp?.epochSecond ?: return null
    val lastPointEpoch = points.last().timestamp.epochSecond
    val end = max(lastPointEpoch, nowEpoch)
    if (end <= first) return null
    return TimelineWindow(startEpoch = first, endEpoch = end)
}

/**
 * Converts the projected series into ON intervals. Each consecutive pair
 * `(p[i], p[i+1])` holds `p[i].value` until `p[i+1]`. We emit an interval
 * for any pair where the held value is ON (projected ≥ 0.5).
 *
 * The trailing point — if it's ON and the chart extends past it — is
 * stretched to [windowEnd] so a currently-ON switch shows as a block
 * that reaches the right edge.
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

/**
 * Computes the next viewport given a single pinch/pan gesture step.
 *
 *  - [zoom] is multiplicative; > 1 zooms in (shrinks visible span), < 1
 *    zooms out (grows it).
 *  - The time point at [centroidFraction] across the chart stays fixed
 *    while the span scales — so the user zooms *into* what they're
 *    pointing at, not the chart centre.
 *  - [panFraction] is the drag delta as a fraction of chart width;
 *    dragging right shifts the viewport left in time (reveal earlier
 *    data), matching every other timeline-style chart.
 *  - The result is clamped to [dataWindow] so the user can't escape the
 *    real data, and to a [MinSpanSeconds] floor so blocks stay readable.
 */
internal fun applyGesture(
    current: TimelineWindow,
    dataWindow: TimelineWindow,
    centroidFraction: Float,
    zoom: Float,
    panFraction: Float
): TimelineWindow {
    val curSpan = current.spanSeconds.toDouble()
    val maxSpan = dataWindow.spanSeconds.toDouble()

    // 1) Zoom anchored at the centroid: time under the fingers stays put.
    val targetSpanRaw = curSpan / zoom.coerceAtLeast(0.001f).toDouble()
    val targetSpan = targetSpanRaw
        .coerceAtLeast(MinSpanSeconds.toDouble())
        .coerceAtMost(maxSpan)
        .toLong()
        .coerceAtLeast(1L)

    val anchorEpoch = current.startEpoch + (centroidFraction * curSpan).toLong()
    var newStart = anchorEpoch - (centroidFraction * targetSpan).toLong()

    // 2) Pan in time units (relative to the *new* span — feels natural
    //    when zoomed in: a short drag moves a short visible span).
    newStart -= (panFraction * targetSpan).toLong()

    // 3) Clamp into data bounds, preserving the span.
    var newEnd = newStart + targetSpan
    if (newEnd > dataWindow.endEpoch) {
        newEnd = dataWindow.endEpoch
        newStart = newEnd - targetSpan
    }
    if (newStart < dataWindow.startEpoch) {
        newStart = dataWindow.startEpoch
        newEnd = (newStart + targetSpan).coerceAtMost(dataWindow.endEpoch)
    }

    return TimelineWindow(newStart, newEnd)
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
    val leftPad = ChartLeftPad.toPx()
    val rightPad = ChartRightPad.toPx()
    val topPad = ChartTopPad.toPx()
    val bottomPad = ChartBottomPad.toPx()

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
        // Skip intervals entirely outside the visible window. Important
        // when zoomed in — we don't want to render hundreds of off-screen
        // blocks pixel-by-pixel.
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

/**
 * Picks a tick label format based on the *visible* span — when the user
 * zooms in past a minute we show seconds, when zoomed all the way out to
 * days we add the weekday. Keying off the range alone would leave us
 * with coarse "HH:mm" labels even when the user has zoomed into a
 * 5-minute window.
 */
private fun timeFormatterFor(spanSeconds: Long): DateTimeFormatter {
    val pattern = when {
        spanSeconds <= 2L * 3_600L -> "HH:mm:ss"   // ≤ 2 h: include seconds
        spanSeconds <= 2L * 86_400L -> "HH:mm"     // ≤ 2 d: hour:minute
        else -> "EEE HH:mm"                        // multi-day: + weekday
    }
    return DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault())
}

// Chart padding constants — shared between the draw scope and the gesture
// handler so the gesture math agrees with where the chart is actually
// drawn.
private val ChartLeftPad: Dp = 36.dp
private val ChartRightPad: Dp = 8.dp
private val ChartTopPad: Dp = 4.dp
private val ChartBottomPad: Dp = 22.dp

/** Floor for `applyGesture`'s span clamp — past this blocks read as a single bar. */
private const val MinSpanSeconds = 60L

/**
 * If the viewport is within this many seconds of the data window's end,
 * we treat it as "fully zoomed out" and re-snap it to the new window on
 * live updates. Generous enough to swallow second-level drift between
 * the 30-second `nowEpoch` tick and the latest live state.
 */
private const val ResnapToleranceSeconds = 60L
