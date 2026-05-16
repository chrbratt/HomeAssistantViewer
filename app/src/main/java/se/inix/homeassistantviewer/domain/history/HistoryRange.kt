package se.inix.homeassistantviewer.domain.history

import java.time.Duration

/**
 * Quick-pick time windows for the entity history chart. Default is [Day]
 * because 24 hours is the most useful granularity for most home automation
 * sensors and switches.
 *
 * Keeping these as a closed enum (rather than a free-form duration picker)
 * means the chart can pre-tune sampling, axis labels and tick spacing per
 * range without runtime branching on arbitrary durations.
 */
enum class HistoryRange(val duration: Duration, val label: String) {
    Hour(Duration.ofHours(1), "1 h"),
    Day(Duration.ofHours(24), "24 h"),
    Week(Duration.ofDays(7), "7 d");

    companion object {
        val Default: HistoryRange = Day
    }
}
