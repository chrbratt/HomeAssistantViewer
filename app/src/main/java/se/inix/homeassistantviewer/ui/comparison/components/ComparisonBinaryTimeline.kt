package se.inix.homeassistantviewer.ui.comparison.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
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
import se.inix.homeassistantviewer.domain.comparison.ComparisonSeriesEntry
import se.inix.homeassistantviewer.domain.history.HistoryRange
import se.inix.homeassistantviewer.ui.detail.components.TimelineChartRightPad
import se.inix.homeassistantviewer.ui.detail.components.TimelineResnapToleranceSeconds
import se.inix.homeassistantviewer.ui.detail.components.TimelineWindow
import se.inix.homeassistantviewer.ui.detail.components.applyGesture
import se.inix.homeassistantviewer.ui.detail.components.computeOnIntervals
import se.inix.homeassistantviewer.ui.detail.components.computeWindow
import se.inix.homeassistantviewer.ui.detail.components.timeFormatterFor
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val SwimlaneLabelWidth = 120.dp
private val SwimlaneTrackHeight = 48.dp
private val SwimlaneRowSpacing = 10.dp
private val SwimlaneTimeAxisHeight = 28.dp
private val SwimlaneColorBarWidth = 4.dp
private val SwimlaneSectionTopPad = 4.dp

@Composable
internal fun ColumnScope.ComparisonBinaryTimelineSection(
    series: List<ComparisonSeriesEntry>,
    range: HistoryRange,
    modifier: Modifier = Modifier,
    chartModifier: Modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
) {
    var hiddenKeys by remember { mutableStateOf(emptySet<String>()) }
    val colors = comparisonSeriesColors(series.size)
    val visibleSeries = series.filter { it.legendKey() !in hiddenKeys }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "On / off",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Text(
            text = "One row per entity — filled is on, empty is off.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp)
        )
        ComparisonLegend(
            series = series,
            colors = colors,
            hiddenKeys = hiddenKeys,
            onToggle = { entry ->
                val key = entry.legendKey()
                hiddenKeys = if (key in hiddenKeys) hiddenKeys - key else hiddenKeys + key
            }
        )
        if (visibleSeries.isNotEmpty()) {
            val rowBlockHeight = SwimlaneTrackHeight * visibleSeries.size +
                SwimlaneRowSpacing * (visibleSeries.size - 1).coerceAtLeast(0)
            ComparisonBinarySwimlanes(
                series = visibleSeries,
                colors = colors,
                range = range,
                modifier = chartModifier
                    .fillMaxWidth()
                    .height(rowBlockHeight + SwimlaneTimeAxisHeight + SwimlaneSectionTopPad)
            )
        }
    }
}

@Composable
private fun ComparisonBinarySwimlanes(
    series: List<ComparisonSeriesEntry>,
    colors: List<Color>,
    range: HistoryRange,
    modifier: Modifier = Modifier
) {
    val nowEpoch by produceState(initialValue = Instant.now().epochSecond) {
        while (true) {
            delay(30_000L)
            value = Instant.now().epochSecond
        }
    }

    val allPoints = remember(series) { series.flatMap { it.series.points } }
    val window = remember(allPoints, nowEpoch) { computeWindow(allPoints, nowEpoch) } ?: return
    val laneData = remember(series, window, nowEpoch) {
        series.map { entry ->
            BinarySwimlaneData(
                label = entry.displayName,
                colorIndex = entry.colorIndex,
                intervals = computeOnIntervals(entry.series.points, window.endEpoch)
            )
        }
    }

    var viewport by remember(range) { mutableStateOf(window) }
    LaunchedEffect(window) {
        val unzoomed = viewport.startEpoch == window.startEpoch &&
            window.endEpoch - viewport.endEpoch <= TimelineResnapToleranceSeconds
        if (unzoomed) viewport = window
    }

    val offTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val guidelineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val mutedLabelStyle = MaterialTheme.typography.labelSmall
        .copy(color = mutedColor, textAlign = TextAlign.Center)
    val textMeasurer = rememberTextMeasurer()
    val timeFormatter by remember {
        derivedStateOf { timeFormatterFor(viewport.spanSeconds) }
    }

    Box(
        modifier = modifier
            .padding(top = SwimlaneSectionTopPad)
            .pointerInput(window) {
                detectTapGestures(onDoubleTap = { viewport = window })
            }
            .pointerInput(Unit) {
                val labelPx = SwimlaneLabelWidth.toPx()
                val rightPx = TimelineChartRightPad.toPx()
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val chartWidth = (size.width - labelPx - rightPx).coerceAtLeast(1f)
                    val centroidFraction = ((centroid.x - labelPx) / chartWidth).coerceIn(0f, 1f)
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SwimlaneRowSpacing)
        ) {
            laneData.forEach { lane ->
                val accentColor = colors.getOrElse(lane.colorIndex) { colors.first() }
                BinarySwimlaneRow(
                    label = lane.label,
                    accentColor = accentColor,
                    intervals = lane.intervals,
                    viewport = viewport,
                    offTrackColor = offTrackColor,
                    borderColor = borderColor
                )
            }
            BinarySharedTimeAxis(
                viewport = viewport,
                textMeasurer = textMeasurer,
                labelStyle = mutedLabelStyle,
                timeFormatter = timeFormatter
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    SwimlaneTrackHeight * laneData.size +
                        SwimlaneRowSpacing * (laneData.size - 1).coerceAtLeast(0)
                )
        ) {
            Spacer(modifier = Modifier.width(SwimlaneLabelWidth))
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = TimelineChartRightPad)
            ) {
                drawSharedVerticalGuides(
                    viewport = viewport,
                    guidelineColor = guidelineColor,
                    laneCount = laneData.size,
                    laneHeightPx = SwimlaneTrackHeight.toPx(),
                    laneSpacingPx = SwimlaneRowSpacing.toPx()
                )
            }
        }
    }
}

@Composable
private fun BinarySwimlaneRow(
    label: String,
    accentColor: Color,
    intervals: List<Pair<Long, Long>>,
    viewport: TimelineWindow,
    offTrackColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SwimlaneTrackHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .width(SwimlaneLabelWidth)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(SwimlaneColorBarWidth)
                    .height(30.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = TimelineChartRightPad)
                .clip(RoundedCornerShape(6.dp))
        ) {
            drawSwimlaneTrack(
                intervals = intervals,
                viewport = viewport,
                offColor = offTrackColor,
                onColor = accentColor,
                borderColor = borderColor
            )
        }
    }
}

@Composable
private fun BinarySharedTimeAxis(
    viewport: TimelineWindow,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    timeFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SwimlaneTimeAxisHeight),
        verticalAlignment = Alignment.Top
    ) {
        Spacer(modifier = Modifier.width(SwimlaneLabelWidth))
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = TimelineChartRightPad)
        ) {
            drawSharedTimeAxis(
                viewport = viewport,
                textMeasurer = textMeasurer,
                labelStyle = labelStyle,
                timeFormatter = timeFormatter
            )
        }
    }
}

private data class BinarySwimlaneData(
    val label: String,
    val colorIndex: Int,
    val intervals: List<Pair<Long, Long>>
)

private fun DrawScope.drawSwimlaneTrack(
    intervals: List<Pair<Long, Long>>,
    viewport: TimelineWindow,
    offColor: Color,
    onColor: Color,
    borderColor: Color
) {
    drawRect(color = offColor, size = size)

    val span = viewport.spanSeconds.toDouble()
    fun xToPixel(epoch: Long): Float {
        val frac = ((epoch - viewport.startEpoch).toDouble() / span).toFloat()
        return frac * size.width
    }

    for ((start, end) in intervals) {
        if (end <= viewport.startEpoch || start >= viewport.endEpoch) continue
        val xs = xToPixel(start.coerceAtLeast(viewport.startEpoch))
        val xe = xToPixel(end.coerceAtMost(viewport.endEpoch))
        if (xe > xs) {
            drawRect(
                color = onColor.copy(alpha = 0.9f),
                topLeft = Offset(xs, 0f),
                size = Size(xe - xs, size.height)
            )
        }
    }

    drawRect(
        color = borderColor,
        size = size,
        style = Stroke(width = 1f)
    )
}

private fun DrawScope.drawSharedVerticalGuides(
    viewport: TimelineWindow,
    guidelineColor: Color,
    laneCount: Int,
    laneHeightPx: Float,
    laneSpacingPx: Float
) {
    if (laneCount <= 0) return

    val span = viewport.spanSeconds.toDouble()
    fun xToPixel(epoch: Long): Float {
        val frac = ((epoch - viewport.startEpoch).toDouble() / span).toFloat()
        return frac * size.width
    }

    val totalHeight = laneCount * laneHeightPx + (laneCount - 1).coerceAtLeast(0) * laneSpacingPx
    val tickCount = 5
    val ticks = List(tickCount) { i ->
        viewport.startEpoch + ((i.toDouble() / (tickCount - 1)) * span).roundToInt()
    }
    for (t in ticks) {
        val x = xToPixel(t)
        drawLine(
            color = guidelineColor,
            start = Offset(x, 0f),
            end = Offset(x, totalHeight),
            strokeWidth = 0.8f
        )
    }
}

private fun DrawScope.drawSharedTimeAxis(
    viewport: TimelineWindow,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    timeFormatter: DateTimeFormatter
) {
    val span = viewport.spanSeconds.toDouble()
    fun xToPixel(epoch: Long): Float {
        val frac = ((epoch - viewport.startEpoch).toDouble() / span).toFloat()
        return frac * size.width
    }

    val tickCount = 5
    val ticks = List(tickCount) { i ->
        viewport.startEpoch + ((i.toDouble() / (tickCount - 1)) * span).roundToInt()
    }
    for (t in ticks) {
        val text = timeFormatter.format(Instant.ofEpochSecond(t))
        val layout = textMeasurer.measure(text, labelStyle)
        val centerX = xToPixel(t)
        val left = (centerX - layout.size.width / 2f)
            .coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(left, 2.dp.toPx())
        )
    }
}
