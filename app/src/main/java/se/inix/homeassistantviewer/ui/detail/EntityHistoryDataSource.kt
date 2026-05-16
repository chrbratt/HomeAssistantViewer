package se.inix.homeassistantviewer.ui.detail

import kotlinx.coroutines.flow.Flow
import se.inix.homeassistantviewer.data.model.HaEntityState
import se.inix.homeassistantviewer.data.model.HaHistoryRow
import se.inix.homeassistantviewer.data.ws.ConnectionPool
import java.time.Instant

/**
 * Small abstraction over the three things [EntityDetailViewModel] needs from
 * the network layer:
 *  1. history fetching for the chart,
 *  2. one-shot current-state fetch for the top-of-screen value,
 *  3. a hot flow of live state changes for the same entity, so the chart's
 *     right edge stays current without polling.
 *
 * Existing as an interface (rather than the VM taking [ConnectionPool] directly)
 * means [EntityDetailViewModel] can be unit-tested with a fake — useful because
 * we don't have a mocking library set up in the project.
 */
internal interface EntityHistoryDataSource {
    suspend fun getHistory(entityId: String, start: Instant, end: Instant): List<HaHistoryRow>
    suspend fun getCurrentState(entityId: String): HaEntityState?
    /**
     * Hot flow of live state-changed events for the connection this data
     * source represents. Callers filter by entityId.
     */
    val stateChanges: Flow<HaEntityState>
}

/**
 * Production implementation routing through the existing [ConnectionPool] for
 * the requested [connectionId]. Returns empty data if the connection is no
 * longer configured (user removed the server between navigation and load).
 */
internal class ConnectionPoolDataSource(
    private val pool: ConnectionPool,
    private val connectionId: String
) : EntityHistoryDataSource {

    private val repository get() = pool.repositoryFor(connectionId)
    private val wsClient get() = pool.wsClientFor(connectionId)

    override suspend fun getHistory(
        entityId: String,
        start: Instant,
        end: Instant
    ): List<HaHistoryRow> = repository?.getHistory(entityId, start, end).orEmpty()

    override suspend fun getCurrentState(entityId: String): HaEntityState? =
        repository?.let {
            runCatching { it.getStatesForEntities(setOf(entityId)).firstOrNull() }.getOrNull()
        }

    override val stateChanges: Flow<HaEntityState>
        get() = wsClient?.stateChanges ?: kotlinx.coroutines.flow.emptyFlow()
}
