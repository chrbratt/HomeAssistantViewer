package se.inix.homeassistantviewer.domain.history

import java.time.Duration

/**
 * Home Assistant's recorder keeps detailed *states* only for a short window
 * (`purge_keep_days`, default 10). Older data survives as aggregated
 * *long-term statistics* (hourly/daily mean·min·max). HA's own UI silently
 * switches to statistics for long ranges; we mirror that here.
 *
 * [wsValue] is the `period` argument for `recorder/statistics_during_period`.
 */
enum class StatisticsPeriod(val wsValue: String) {
    Hour("hour"),
    Day("day")
}

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
    Week(Duration.ofDays(7), "7 d"),
    Month(Duration.ofDays(30), "30 d"),
    SixMonths(Duration.ofDays(180), "6 mo"),
    Year(Duration.ofDays(365), "1 yr");

    /** Bottom-axis label pattern for numeric Vico charts. */
    val axisTimePattern: String
        get() = when (this) {
            Hour -> "HH:mm:ss"
            Day -> "HH:mm"
            Week -> "EEE HH:mm"
            Month, SixMonths -> "d MMM"
            Year -> "MMM yy"
        }

    /** Touch-marker time pattern (more detail where space allows). */
    val markerTimePattern: String
        get() = when (this) {
            Hour -> "HH:mm:ss"
            Day -> "HH:mm"
            Week -> "EEE HH:mm"
            Month -> "d MMM HH:mm"
            SixMonths, Year -> "d MMM yyyy"
        }

    /** Epoch second of the window start when [nowEpoch] is the window end. */
    fun startEpoch(nowEpoch: Long): Long = nowEpoch - duration.seconds

    /**
     * Whether this range should be served from long-term statistics instead
     * of raw states, and at what aggregation period. `null` means "use raw
     * states" — accurate and recent enough for short windows that fit inside
     * the recorder's purge window.
     */
    val statisticsPeriod: StatisticsPeriod?
        get() = when (this) {
            Hour, Day, Week -> null
            // Hourly long-term statistics give ~24 points/day — far more
            // detail than daily means. Year falls back to daily because
            // hourly stats typically don't reach a full year back.
            Month, SixMonths -> StatisticsPeriod.Hour
            Year -> StatisticsPeriod.Day
        }

    companion object {
        val Default: HistoryRange = Day
    }
}
