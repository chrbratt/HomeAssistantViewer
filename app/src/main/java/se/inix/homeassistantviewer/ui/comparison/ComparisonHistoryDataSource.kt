package se.inix.homeassistantviewer.ui.comparison

import se.inix.homeassistantviewer.data.model.HaEntityState
import se.inix.homeassistantviewer.data.model.HaHistoryRow
import se.inix.homeassistantviewer.data.ws.ConnectionPool
import java.time.Instant

/**
 * History fetcher for the comparison screen — routes each entity to its
 * owning connection in [ConnectionPool].
 */
interface ComparisonHistoryDataSource {
    suspend fun getHistory(
        connectionId: String,
        entityId: String,
        start: Instant,
        end: Instant
    ): List<HaHistoryRow>

    suspend fun getCurrentState(connectionId: String, entityId: String): HaEntityState?
}

internal class PoolComparisonHistoryDataSource(
    private val pool: ConnectionPool
) : ComparisonHistoryDataSource {

    override suspend fun getHistory(
        connectionId: String,
        entityId: String,
        start: Instant,
        end: Instant
    ): List<HaHistoryRow> =
        pool.repositoryFor(connectionId)?.getHistory(entityId, start, end).orEmpty()

    override suspend fun getCurrentState(connectionId: String, entityId: String): HaEntityState? =
        pool.repositoryFor(connectionId)?.let { repo ->
            runCatching { repo.getStatesForEntities(setOf(entityId)).firstOrNull() }.getOrNull()
        }
}
