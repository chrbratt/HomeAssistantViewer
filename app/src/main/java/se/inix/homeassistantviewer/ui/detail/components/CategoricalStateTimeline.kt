package se.inix.homeassistantviewer.ui.detail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import se.inix.homeassistantviewer.domain.history.HistoryPoint
import se.inix.homeassistantviewer.domain.history.HistoryRange
import se.inix.homeassistantviewer.domain.history.HistorySeries
import se.inix.homeassistantviewer.domain.history.SeriesKind
import se.inix.homeassistantviewer.domain.history.StateInterval
import se.inix.homeassistantviewer.domain.history.computeStateIntervals
import se.inix.homeassistantviewer.domain.history.isPlottableHistoryState
import se.inix.homeassistantviewer.ui.common.formatSensorValue
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Home Assistant–style state timeline for entities with named text states
 * (select, weather, device_tracker, text sensors without units, …).
 *
 * Each distinct state gets a colour; contiguous periods render as filled
 * blocks. Pinch/zoom/pan behaviour matches [BinaryStateTimeline].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoricalStateTimeline(
    series: HistorySeries,
    range: HistoryRange,
    modifier: Modifier = Modifier
) {
    val kind = series.kind as? SeriesKind.Categorical ?: return
    val legendStates = remember(series.points, kind.states) {
        buildLegendStates(kind.states, series.points)
    }
    val stateColors = rememberStateColorMap(legendStates)

    val nowEpoch by produceState(initialValue = Instant.now().epochSecond) {
        while (true) {
            delay(30_000L)
            value = Instant.now().epochSecond
        }
    }

    val window = remember(series, nowEpoch) { computeWindow(series.points, nowEpoch) } ?: return
    val intervals = remember(series, window) {
        computeStateIntervals(series.points, windowEnd = window.endEpoch)
    }

    var viewport by remember(range) { mutableStateOf(window) }
    LaunchedEffect(window) {
        val unzoomed = viewport.startEpoch == window.startEpoch &&
            window.endEpoch - viewport.endEpoch <= TimelineResnapToleranceSeconds
        if (unzoomed) viewport = window
    }

    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val guidelineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val mutedLabelStyle = MaterialTheme.typography.labelSmall
        .copy(color = mutedColor, textAlign = TextAlign.Center)
    val blockLabelStyle = MaterialTheme.typography.labelSmall

    val textMeasurer = rememberTextMeasurer()
    val timeFormatter by remember {
        derivedStateOf { timeFormatterFor(viewport.spanSeconds) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(window) {
                    detectTapGestures(onDoubleTap = { viewport = window })
                }
                .pointerInput(Unit) {
                    val leftPx = TimelineChartLeftPad.toPx()
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
            drawCategoricalStateTimeline(
                intervals = intervals,
                viewport = viewport,
                stateColors = stateColors,
                blockLabelStyle = blockLabelStyle,
                mutedLabelStyle = mutedLabelStyle,
                guidelineColor = guidelineColor,
                borderColor = borderColor,
                textMeasurer = textMeasurer,
                timeFormatter = timeFormatter
            )
        }

        if (legendStates.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                legendStates.forEach { state ->
                    val color = stateColors[state] ?: MaterialTheme.colorScheme.primary
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawRect(color = color.copy(alpha = 0.85f))
                        }
                        Text(
                            text = formatSensorValue(state),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** Merges build-time states with any values seen in points (incl. live WS appends). */
private fun buildLegendStates(
    catalogStates: List<String>,
    points: List<HistoryPoint>
): List<String> = buildList {
    catalogStates.forEach { add(it) }
    points.forEach { point ->
        if (isPlottableHistoryState(point.rawState) && point.rawState !in this) {
            add(point.rawState)
        }
    }
}

@Composable
private fun rememberStateColorMap(states: List<String>): Map<String, Color> {
    val scheme = MaterialTheme.colorScheme
    val palette = listOf(
        scheme.primary,
        scheme.secondary,
        scheme.tertiary,
        scheme.error,
        scheme.primaryContainer,
        scheme.secondaryContainer,
        scheme.tertiaryContainer,
        scheme.inversePrimary
    )
    return remember(states, palette) {
        states.withIndex().associate { (index, state) ->
            state to palette[index % palette.size]
        }
    }
}

private fun DrawScope.drawCategoricalStateTimeline(
    intervals: List<StateInterval>,
    viewport: TimelineWindow,
    stateColors: Map<String, Color>,
    blockLabelStyle: TextStyle,
    mutedLabelStyle: TextStyle,
    guidelineColor: Color,
    borderColor: Color,
    textMeasurer: TextMeasurer,
    timeFormatter: DateTimeFormatter
) {
    val leftPad = TimelineChartLeftPad.toPx()
    val rightPad = TimelineChartRightPad.toPx()
    val topPad = TimelineChartTopPad.toPx()
    val bottomPad = TimelineChartBottomPad.toPx()

    val chartLeft = leftPad
    val chartRight = size.width - rightPad
    val chartTop = topPad
    val chartBottom = size.height - bottomPad
    val chartHeight = chartBottom - chartTop
    val span = viewport.spanSeconds.toDouble()
    val minLabelWidthPx = 48.dp.toPx()

    fun xToPixel(epoch: Long): Float {
        val frac = ((epoch - viewport.startEpoch).toDouble() / span).toFloat()
        return chartLeft + frac * (chartRight - chartLeft)
    }

    for (interval in intervals) {
        if (interval.endEpoch <= viewport.startEpoch || interval.startEpoch >= viewport.endEpoch) {
            continue
        }
        val xs = xToPixel(interval.startEpoch.coerceAtLeast(viewport.startEpoch))
        val xe = xToPixel(interval.endEpoch.coerceAtMost(viewport.endEpoch))
        if (xe <= xs) continue

        val color = (stateColors[interval.state] ?: Color.Gray).copy(alpha = 0.85f)
        drawRect(
            color = color,
            topLeft = Offset(xs, chartTop),
            size = Size(xe - xs, chartHeight)
        )

        val blockWidth = xe - xs
        if (blockWidth >= minLabelWidthPx) {
            val label = formatSensorValue(interval.state)
            val labelStyle = blockLabelStyle.copy(
                color = if (color.luminance() > 0.55f) Color.Black else Color.White
            )
            val layout = textMeasurer.measure(label, labelStyle, maxLines = 1)
            if (layout.size.width <= blockWidth - 4.dp.toPx()) {
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        xs + (blockWidth - layout.size.width) / 2f,
                        chartTop + (chartHeight - layout.size.height) / 2f
                    )
                )
            }
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
