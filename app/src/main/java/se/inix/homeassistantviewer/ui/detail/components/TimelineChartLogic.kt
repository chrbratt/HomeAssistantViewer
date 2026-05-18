package se.inix.homeassistantviewer.ui.detail.components

import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.domain.history.HistoryPoint
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

/** Visible time range for state timelines (epoch seconds). */
internal data class TimelineWindow(val startEpoch: Long, val endEpoch: Long) {
    val spanSeconds: Long get() = (endEpoch - startEpoch).coerceAtLeast(1L)
}

/**
 * Picks the chart window. We pin the left edge at the first known data
 * point and the right edge at max(last data, now).
 */
internal fun computeWindow(points: List<HistoryPoint>, nowEpoch: Long): TimelineWindow? {
    val first = points.firstOrNull()?.timestamp?.epochSecond ?: return null
    val lastPointEpoch = points.last().timestamp.epochSecond
    val end = max(lastPointEpoch, nowEpoch)
    if (end <= first) return null
    return TimelineWindow(startEpoch = first, endEpoch = end)
}

internal fun applyGesture(
    current: TimelineWindow,
    dataWindow: TimelineWindow,
    centroidFraction: Float,
    zoom: Float,
    panFraction: Float
): TimelineWindow {
    val curSpan = current.spanSeconds.toDouble()
    val maxSpan = dataWindow.spanSeconds.toDouble()

    val targetSpanRaw = curSpan / zoom.coerceAtLeast(0.001f).toDouble()
    val targetSpan = targetSpanRaw
        .coerceAtLeast(TimelineMinSpanSeconds.toDouble())
        .coerceAtMost(maxSpan)
        .toLong()
        .coerceAtLeast(1L)

    val anchorEpoch = current.startEpoch + (centroidFraction * curSpan).toLong()
    var newStart = anchorEpoch - (centroidFraction * targetSpan).toLong()
    newStart -= (panFraction * targetSpan).toLong()

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

internal fun timeFormatterFor(spanSeconds: Long): DateTimeFormatter {
    val pattern = when {
        spanSeconds <= 2L * 3_600L -> "HH:mm:ss"
        spanSeconds <= 2L * 86_400L -> "HH:mm"
        spanSeconds <= 7L * 86_400L -> "EEE HH:mm"
        else -> "d MMM"
    }
    return DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault())
}

internal val TimelineChartLeftPad = 8.dp
internal val TimelineChartRightPad = 8.dp
internal val TimelineChartTopPad = 4.dp
internal val TimelineChartBottomPad = 22.dp

internal const val TimelineMinSpanSeconds = 60L
internal const val TimelineResnapToleranceSeconds = 60L
