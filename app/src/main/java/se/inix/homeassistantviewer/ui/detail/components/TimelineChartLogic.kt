package se.inix.homeassistantviewer.ui.detail.components

import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.domain.history.HistoryPoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

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

/**
 * Candidate tick steps (seconds) that produce round wall-clock labels:
 * whole seconds → minutes → hours → days. Picking the smallest candidate
 * that yields roughly [targetTicks] labels keeps tick spacing stable while
 * zooming instead of landing on arbitrary epochs.
 */
private val NiceTimeStepsSeconds = longArrayOf(
    1, 2, 5, 10, 15, 30,
    60, 2 * 60, 5 * 60, 10 * 60, 15 * 60, 30 * 60,
    3600, 2 * 3600, 3 * 3600, 6 * 3600, 12 * 3600,
    86_400, 2 * 86_400, 7 * 86_400, 14 * 86_400, 30 * 86_400
)

/** Smallest "nice" step (seconds) that splits [spanSeconds] into ~[targetTicks] intervals. */
internal fun niceTimeStepSeconds(spanSeconds: Long, targetTicks: Int = 5): Long {
    val raw = (spanSeconds.toDouble() / targetTicks.coerceAtLeast(1)).coerceAtLeast(1.0)
    return NiceTimeStepsSeconds.firstOrNull { it >= raw } ?: NiceTimeStepsSeconds.last()
}

/**
 * Tick epochs snapped to round local-time boundaries (e.g. whole minutes /
 * hours) within `[startEpoch, endEpoch]`. The local offset is folded in
 * before snapping so a 1-hour step lands on `14:00`, not on a UTC multiple.
 */
internal fun niceTimeTicks(
    startEpoch: Long,
    endEpoch: Long,
    targetTicks: Int = 5,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<Long> {
    if (endEpoch <= startEpoch) return listOf(startEpoch)
    val step = niceTimeStepSeconds(endEpoch - startEpoch, targetTicks)
    val offsetSeconds = zoneId.rules.getOffset(Instant.ofEpochSecond(startEpoch)).totalSeconds.toLong()
    val localStart = startEpoch + offsetSeconds
    var localTick = Math.floorDiv(localStart + step - 1, step) * step
    val ticks = mutableListOf<Long>()
    while (ticks.size < 512) {
        val epoch = localTick - offsetSeconds
        if (epoch > endEpoch) break
        if (epoch in startEpoch..endEpoch) ticks += epoch
        localTick += step
    }
    return ticks.ifEmpty { listOf(startEpoch) }
}

/** Convenience overload for a [TimelineWindow] viewport. */
internal fun niceTimeTicks(
    window: TimelineWindow,
    targetTicks: Int = 5,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<Long> = niceTimeTicks(window.startEpoch, window.endEpoch, targetTicks, zoneId)

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
