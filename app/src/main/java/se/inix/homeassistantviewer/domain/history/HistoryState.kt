package se.inix.homeassistantviewer.domain.history

/**
 * One contiguous period where an entity held a named state.
 * Used by the categorical history timeline (select, weather, text sensors, …).
 */
data class StateInterval(
    val startEpoch: Long,
    val endEpoch: Long,
    val state: String
)

private val NON_PLOTTABLE_STATES = setOf("unavailable", "unknown", "none")

/** Whether [rawState] should appear as a filled block on a state timeline. */
fun isPlottableHistoryState(rawState: String): Boolean {
    if (rawState.isBlank()) return false
    return rawState.lowercase() !in NON_PLOTTABLE_STATES
}

/**
 * Converts a chronological point series into state-holding intervals.
 * Each point holds its [HistoryPoint.rawState] until the next change.
 * The trailing point extends to [windowEnd] so the current state reaches
 * the chart's right edge.
 */
internal fun computeStateIntervals(
    points: List<HistoryPoint>,
    windowEnd: Long
): List<StateInterval> {
    if (points.isEmpty()) return emptyList()
    val intervals = mutableListOf<StateInterval>()
    for (i in 0 until points.size - 1) {
        val state = points[i].rawState
        if (!isPlottableHistoryState(state)) continue
        val start = points[i].timestamp.epochSecond
        val end = points[i + 1].timestamp.epochSecond
        if (end > start) intervals += StateInterval(start, end, state)
    }
    val last = points.last()
    if (isPlottableHistoryState(last.rawState)) {
        val start = last.timestamp.epochSecond
        if (windowEnd > start) intervals += StateInterval(start, windowEnd, last.rawState)
    }
    return intervals
}

/** Distinct plottable states in first-seen order — drives legend and colours. */
internal fun orderedDistinctStates(points: List<HistoryPoint>): List<String> {
    val seen = LinkedHashSet<String>()
    for (p in points) {
        if (isPlottableHistoryState(p.rawState)) seen.add(p.rawState)
    }
    return seen.toList()
}
