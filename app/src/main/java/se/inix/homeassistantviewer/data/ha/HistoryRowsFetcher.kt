package se.inix.homeassistantviewer.data.ha

import se.inix.homeassistantviewer.data.model.HaHistoryRow
import se.inix.homeassistantviewer.data.model.HaStatisticsConverter
import se.inix.homeassistantviewer.data.ws.ConnectionPool
import se.inix.homeassistantviewer.domain.history.StatisticsPeriod
import java.time.Instant

/**
 * Single policy for "raw states vs long-term statistics", shared by the
 * detail and comparison data sources so the rule lives in exactly one place.
 *
 * When [statisticsPeriod] is set (long ranges), we ask the WebSocket for
 * aggregated statistics first — that's the only source for data the recorder
 * has already purged from the states table. If statistics are unavailable
 * (socket down, or the entity has no `state_class`), we fall back to raw
 * states so short-retention setups still show whatever recent data exists.
 */
suspend fun fetchHistoryRows(
    pool: ConnectionPool,
    connectionId: String,
    entityId: String,
    start: Instant,
    end: Instant,
    statisticsPeriod: StatisticsPeriod?
): List<HaHistoryRow> {
    if (statisticsPeriod != null) {
        val stats = pool.wsClientFor(connectionId)?.let { ws ->
            runCatching { ws.getStatistics(entityId, start, end, statisticsPeriod.wsValue) }
                .getOrNull()
        }
        if (!stats.isNullOrEmpty()) return HaStatisticsConverter.toHistoryRows(stats)
    }
    return pool.repositoryFor(connectionId)?.getHistory(entityId, start, end).orEmpty()
}
