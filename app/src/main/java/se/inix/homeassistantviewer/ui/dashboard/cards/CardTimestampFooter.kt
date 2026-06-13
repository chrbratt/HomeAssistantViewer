package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import se.inix.homeassistantviewer.data.settings.CardTimestamp
import se.inix.homeassistantviewer.ui.common.formatRelativeShort
import se.inix.homeassistantviewer.ui.common.relativeRefreshIntervalMs
import se.inix.homeassistantviewer.ui.dashboard.DashboardItem
import java.time.Instant

/**
 * The instant a card should show in its footer, or `null` to show no footer.
 *
 * A stale entity always reports how long ago its retained value was real
 * (regardless of the timestamp setting), so the "outdated" marker is never
 * hidden. Otherwise the resolved [DashboardItem.Entity.timestampMode] decides:
 * `last_reported` falls back to `last_updated` when HA doesn't provide it.
 */
internal fun DashboardItem.Entity.footerTimestamp(): Instant? {
    val e = entity ?: return null
    return when {
        isStale -> e.lastUpdatedInstant
        timestampMode == CardTimestamp.LAST_UPDATED -> e.lastUpdatedInstant
        timestampMode == CardTimestamp.LAST_REPORTED -> e.lastReportedInstant ?: e.lastUpdatedInstant
        else -> null
    }
}

/**
 * Subtle "time since last update" line shown at the bottom of a card.
 *
 * Two roles, both deliberately understated:
 *  - when [isStale] the entity is currently `unavailable`/`unknown`; the card
 *    above keeps the last known value (dimmed) and this footer shows how long
 *    ago that value was real, with a "cloud off" marker.
 *  - otherwise it's the opt-in timestamp counter (global or per-entity).
 *
 * The label self-refreshes on a cadence that matches its own granularity
 * (see [relativeRefreshIntervalMs]) so it never busy-loops.
 */
@Composable
internal fun CardTimestampFooter(
    timestamp: Instant,
    isStale: Boolean,
    modifier: Modifier = Modifier
) {
    val elapsedSeconds by produceState(
        initialValue = secondsSince(timestamp),
        key1 = timestamp
    ) {
        while (true) {
            val current = secondsSince(timestamp)
            value = current
            delay(relativeRefreshIntervalMs(current))
        }
    }

    val tint = if (isStale) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    }
    val label = if (isStale) {
        "Outdated · ${formatRelativeShort(elapsedSeconds)}"
    } else {
        formatRelativeShort(elapsedSeconds)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isStale) Icons.Rounded.CloudOff else Icons.Rounded.Schedule,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

private fun secondsSince(instant: Instant): Long =
    (System.currentTimeMillis() - instant.toEpochMilli()) / 1000L
