package se.inix.homeassistantviewer.ui.common

/**
 * Formats an elapsed duration as a compact, glanceable "time since" label
 * (e.g. `now`, `42s`, `5m`, `3h`, `2d`).
 *
 * Kept pure (no Android / Compose deps) so the thresholds can be unit-tested
 * directly. Negative inputs — a clock that's slightly ahead of the device —
 * are clamped to `now` rather than rendering a nonsensical negative age.
 */
fun formatRelativeShort(elapsedSeconds: Long): String {
    val s = elapsedSeconds.coerceAtLeast(0)
    return when {
        s < 10 -> "now"
        s < 60 -> "${s}s"
        s < 3_600 -> "${s / 60}m"
        s < 86_400 -> "${s / 3_600}h"
        else -> "${s / 86_400}d"
    }
}

/**
 * How long until [formatRelativeShort] would produce a different string, so a
 * live label can schedule its next refresh without busy-looping: per-second
 * under a minute, per-minute under an hour, then hourly.
 */
fun relativeRefreshIntervalMs(elapsedSeconds: Long): Long {
    val s = elapsedSeconds.coerceAtLeast(0)
    return when {
        s < 60 -> 1_000L
        s < 3_600 -> 30_000L
        else -> 300_000L
    }
}
