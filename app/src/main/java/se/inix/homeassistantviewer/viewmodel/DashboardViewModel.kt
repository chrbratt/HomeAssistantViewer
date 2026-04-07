package se.inix.homeassistantviewer.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.inix.homeassistantviewer.data.ConnectionPool
import se.inix.homeassistantviewer.data.ConnectionState
import se.inix.homeassistantviewer.data.SettingsRepository
import se.inix.homeassistantviewer.data.model.FavoriteEntity
import se.inix.homeassistantviewer.data.model.HaEntityState

/**
 * An entity on the dashboard.
 * [entity] is null while the state has not yet been loaded or when the connection is offline.
 * [entityId] is always present so the card can be shown even when [entity] is unavailable.
 */
data class DashboardEntity(
    val connectionId: String,
    val entityId: String,
    val entity: HaEntityState?
)

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    /** No Home Assistant connections have been configured yet. */
    data object NoConnections : DashboardUiState()
    data object NoFavorites : DashboardUiState()
    data class Success(val entities: List<DashboardEntity>) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

private sealed class InitState {
    data object Loading : InitState()
    data class Error(val message: String) : InitState()
    data object Ready : InitState()
}

sealed class EntityAction {
    abstract val connectionId: String
    abstract val entityId: String

    data class Toggle(override val connectionId: String, override val entityId: String) : EntityAction()
    data class SetBrightness(override val connectionId: String, override val entityId: String, val pct: Int) : EntityAction()
    data class OpenCover(override val connectionId: String, override val entityId: String) : EntityAction()
    data class CloseCover(override val connectionId: String, override val entityId: String) : EntityAction()
    data class StopCover(override val connectionId: String, override val entityId: String) : EntityAction()
    data class SetCoverPosition(override val connectionId: String, override val entityId: String, val position: Int) : EntityAction()
    data class SetClimateTemperature(override val connectionId: String, override val entityId: String, val temperature: Double) : EntityAction()
    data class SetClimateHvacMode(override val connectionId: String, override val entityId: String, val mode: String) : EntityAction()
    data class SetFanPercentage(override val connectionId: String, override val entityId: String, val percentage: Int) : EntityAction()
    data class Lock(override val connectionId: String, override val entityId: String) : EntityAction()
    data class Unlock(override val connectionId: String, override val entityId: String) : EntityAction()
    data class MediaPlayPause(override val connectionId: String, override val entityId: String) : EntityAction()
    data class MediaPrevious(override val connectionId: String, override val entityId: String) : EntityAction()
    data class MediaNext(override val connectionId: String, override val entityId: String) : EntityAction()
    data class SetMediaVolume(override val connectionId: String, override val entityId: String, val volume: Float) : EntityAction()
    data class Activate(override val connectionId: String, override val entityId: String) : EntityAction()
    data class SetInputNumber(override val connectionId: String, override val entityId: String, val value: Double) : EntityAction()
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val connectionPool: ConnectionPool,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    /** Key: FavoriteEntity(connectionId, entityId) → latest known state */
    private val _entityStateMap = MutableStateFlow<Map<FavoriteEntity, HaEntityState>>(emptyMap())
    private val _initState = MutableStateFlow<InitState>(InitState.Loading)

    /**
     * Always exposes ALL favorited entities. Those whose state has not yet been loaded
     * (or whose connection is offline) will have [DashboardEntity.entity] == null so
     * the UI can show a clear "Unavailable" card rather than hiding the entry silently.
     */
    val uiState: StateFlow<DashboardUiState> = combine(
        _entityStateMap,
        settingsRepository.favoriteEntities,
        settingsRepository.connections,
        _initState
    ) { stateMap, favorites, connections, initState ->
        when {
            connections.isEmpty() -> DashboardUiState.NoConnections
            favorites.isEmpty() -> DashboardUiState.NoFavorites
            initState is InitState.Loading -> DashboardUiState.Loading
            initState is InitState.Error -> DashboardUiState.Error(initState.message)
            else -> DashboardUiState.Success(
                entities = favorites.map { fav ->
                    DashboardEntity(
                        connectionId = fav.connectionId,
                        entityId = fav.entityId,
                        entity = stateMap[fav]
                    )
                }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState.Loading)

    val dashboardColumns: StateFlow<Int> = settingsRepository.dashboardColumns

    /**
     * Aggregated live connection state across all configured connections.
     */
    val connectionState: StateFlow<ConnectionState> = connectionPool.clients
        .flatMapLatest { clients ->
            if (clients.isEmpty()) return@flatMapLatest flowOf<ConnectionState>(ConnectionState.Disconnected)
            channelFlow {
                val perConnectionState = mutableMapOf<String, ConnectionState>()
                clients.forEach { (connId, pair) ->
                    launch {
                        pair.wsClient.connectionState.collect { state ->
                            perConnectionState[connId] = state
                            send(aggregateStates(perConnectionState.values))
                        }
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConnectionState.Disconnected)

    init {
        observePoolReadiness()
        fetchOnReconnect()
        collectWebSocketUpdates()
        watchFavoritesChanges()
    }

    /** Full reload — blanks the screen with a spinner (initial load and hard retry). */
    fun fetchStates() = fetchInitialSnapshot()

    /**
     * Pull-to-refresh — keeps existing cards visible while re-fetching in the background.
     * [_initState] is not touched so the grid stays on screen; only the pull indicator moves.
     */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            runCatching { fetchStatesInternal() }
                .onSuccess { map -> if (map.isNotEmpty()) _entityStateMap.value = map }
                .onFailure { e -> Log.w(TAG, "Pull-to-refresh failed", e) }
            _isRefreshing.value = false
        }
    }

    /** Persists the new card order after the user finishes a drag-to-reorder gesture. */
    fun saveFavoriteOrder(ordered: List<DashboardEntity>) {
        settingsRepository.saveFavoriteOrder(
            ordered.map { FavoriteEntity(it.connectionId, it.entityId) }
        )
    }

    /**
     * Watches the connection pool for readiness and triggers a snapshot fetch whenever
     * the set of active connections changes.
     *
     * This solves the startup race where [ConnectionPool] populates its clients
     * asynchronously on [kotlinx.coroutines.Dispatchers.IO]. Calling [fetchInitialSnapshot]
     * directly in `init` would always see an empty pool and return no states.
     */
    private fun observePoolReadiness() {
        viewModelScope.launch {
            connectionPool.clients
                .map { it.keys }
                .distinctUntilChanged()
                .collect { keys ->
                    if (keys.isEmpty()) {
                        // No connections are configured — nothing to fetch, unblock the UI.
                        _initState.value = InitState.Ready
                    } else {
                        fetchInitialSnapshot()
                    }
                }
        }
    }

    /**
     * Re-fetches all entity states each time any connection (re-)authenticates successfully.
     * This ensures changes that occurred while the app was offline or the WebSocket was
     * reconnecting are recovered without waiting for the next push event.
     */
    private fun fetchOnReconnect() {
        viewModelScope.launch {
            connectionState
                .drop(1) // skip the initial Disconnected emission at startup
                .filter { it is ConnectionState.Connected }
                .collect { fetchInitialSnapshot() }
        }
    }

    /**
     * Fetches the current state for all favorited entities in parallel across connections.
     * Returns a map that callers can apply to [_entityStateMap].
     */
    private suspend fun fetchStatesInternal(): Map<FavoriteEntity, HaEntityState> {
        val favorites = settingsRepository.favoriteEntities.value
        if (favorites.isEmpty()) return emptyMap()
        return coroutineScope {
            favorites.groupBy { it.connectionId }
                .map { (connId, favs) ->
                    async {
                        val repo = connectionPool.repositoryFor(connId)
                            ?: return@async emptyList<Pair<FavoriteEntity, HaEntityState>>()
                        runCatching {
                            repo.getStatesForEntities(favs.map { it.entityId }.toSet())
                                .map { FavoriteEntity(connId, it.entityId) to it }
                        }.getOrElse { e ->
                            Log.w(TAG, "Snapshot failed for connection $connId", e)
                            emptyList()
                        }
                    }
                }
                .awaitAll()
                .flatten()
                .toMap()
        }
    }

    /**
     * Full reload with Loading spinner — used on startup and when the pool becomes ready.
     * Each connection is contacted in parallel; individual failures are tolerated.
     */
    private fun fetchInitialSnapshot() {
        viewModelScope.launch {
            val favorites = settingsRepository.favoriteEntities.value
            if (favorites.isEmpty()) {
                _initState.value = InitState.Ready
                return@launch
            }
            _initState.value = InitState.Loading
            runCatching { fetchStatesInternal() }.fold(
                onSuccess = { map ->
                    _entityStateMap.value = map
                    _initState.value = InitState.Ready
                },
                onFailure = { e ->
                    Log.e(TAG, "All snapshots failed", e)
                    _initState.value = InitState.Ready
                }
            )
        }
    }

    /**
     * Collects real-time entity updates from every connection's WebSocket in parallel.
     * [collectLatest] restarts the whole block when the pool's client map changes.
     */
    private fun collectWebSocketUpdates() {
        viewModelScope.launch {
            connectionPool.clients.collectLatest { clients ->
                coroutineScope {
                    clients.forEach { (connectionId, pair) ->
                        launch {
                            pair.wsClient.stateChanges.collect { entity ->
                                val key = FavoriteEntity(connectionId, entity.entityId)
                                if (key in settingsRepository.favoriteEntities.value) {
                                    _entityStateMap.update { it + (key to entity) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Reacts when the user adds or removes favorites.
     * [drop(1)] skips the initial emission, which is already handled by [observePoolReadiness].
     */
    private fun watchFavoritesChanges() {
        viewModelScope.launch {
            settingsRepository.favoriteEntities
                .drop(1)
                .collect {
                    runCatching { fetchStatesInternal() }
                        .onSuccess { map ->
                            if (map.isNotEmpty()) {
                                _entityStateMap.update { current -> current + map }
                            }
                            _initState.value = InitState.Ready
                        }
                        .onFailure { e -> Log.w(TAG, "Favorites refresh failed", e) }
                }
        }
    }

    fun performAction(action: EntityAction) {
        viewModelScope.launch {
            val connectionId = action.connectionId
            val entityId = action.entityId
            val key = FavoriteEntity(connectionId, entityId)
            val current = _entityStateMap.value[key]
            val repo = connectionPool.repositoryFor(connectionId)

            when (action) {
                // ── Toggle (light, switch, input_boolean, automation, fan) ────
                is EntityAction.Toggle -> {
                    current ?: return@launch
                    _entityStateMap.update { map ->
                        map + (key to current.copy(
                            state = if (current.state == "on") "off" else "on"
                        ))
                    }
                    if (repo == null) {
                        _entityStateMap.update { map -> map + (key to current) }
                        return@launch
                    }
                    val domain = entityId.substringBefore(".")
                    runCatching { repo.callService(domain, "toggle", entityId) }
                        .onFailure { e ->
                            Log.e(TAG, "Toggle failed $entityId", e)
                            _entityStateMap.update { map -> map + (key to current) }
                        }
                }

                // ── Light brightness ──────────────────────────────────────────
                is EntityAction.SetBrightness -> {
                    current ?: return@launch
                    repo ?: return@launch
                    val raw = (action.pct / 100.0 * 255.0).toInt().coerceIn(0, 255).toDouble()
                    _entityStateMap.update { map ->
                        map + (key to current.copy(
                            state = "on",
                            attributes = (current.attributes ?: emptyMap()) +
                                mapOf("brightness" to raw)
                        ))
                    }
                    runCatching { repo.setLightBrightness(entityId, action.pct) }
                        .onFailure { e ->
                            Log.e(TAG, "SetBrightness failed $entityId", e)
                            _entityStateMap.update { map -> map + (key to current) }
                        }
                }

                // ── Cover ─────────────────────────────────────────────────────
                is EntityAction.OpenCover -> {
                    repo ?: return@launch
                    runCatching { repo.callService("cover", "open_cover", entityId) }
                        .onFailure { e -> Log.e(TAG, "OpenCover failed $entityId", e) }
                }
                is EntityAction.CloseCover -> {
                    repo ?: return@launch
                    runCatching { repo.callService("cover", "close_cover", entityId) }
                        .onFailure { e -> Log.e(TAG, "CloseCover failed $entityId", e) }
                }
                is EntityAction.StopCover -> {
                    repo ?: return@launch
                    runCatching { repo.callService("cover", "stop_cover", entityId) }
                        .onFailure { e -> Log.e(TAG, "StopCover failed $entityId", e) }
                }
                is EntityAction.SetCoverPosition -> {
                    current ?: return@launch
                    repo ?: return@launch
                    _entityStateMap.update { map ->
                        map + (key to current.copy(
                            attributes = (current.attributes ?: emptyMap()) +
                                mapOf("current_position" to action.position.toDouble())
                        ))
                    }
                    runCatching { repo.setCoverPosition(entityId, action.position) }
                        .onFailure { e ->
                            Log.e(TAG, "SetCoverPosition failed $entityId", e)
                            _entityStateMap.update { map -> map + (key to current) }
                        }
                }

                // ── Climate ───────────────────────────────────────────────────
                is EntityAction.SetClimateTemperature -> {
                    current ?: return@launch
                    repo ?: return@launch
                    _entityStateMap.update { map ->
                        map + (key to current.copy(
                            attributes = (current.attributes ?: emptyMap()) +
                                mapOf("temperature" to action.temperature)
                        ))
                    }
                    runCatching { repo.setClimateTemperature(entityId, action.temperature) }
                        .onFailure { e ->
                            Log.e(TAG, "SetClimateTemp failed $entityId", e)
                            _entityStateMap.update { map -> map + (key to current) }
                        }
                }
                is EntityAction.SetClimateHvacMode -> {
                    current ?: return@launch
                    repo ?: return@launch
                    _entityStateMap.update { map ->
                        map + (key to current.copy(state = action.mode))
                    }
                    runCatching { repo.setClimateHvacMode(entityId, action.mode) }
                        .onFailure { e ->
                            Log.e(TAG, "SetClimateMode failed $entityId", e)
                            _entityStateMap.update { map -> map + (key to current) }
                        }
                }

                // ── Fan speed ─────────────────────────────────────────────────
                is EntityAction.SetFanPercentage -> {
                    current ?: return@launch
                    repo ?: return@launch
                    _entityStateMap.update { map ->
                        map + (key to current.copy(
                            attributes = (current.attributes ?: emptyMap()) +
                                mapOf("percentage" to action.percentage.toDouble())
                        ))
                    }
                    runCatching { repo.setFanPercentage(entityId, action.percentage) }
                        .onFailure { e ->
                            Log.e(TAG, "SetFanPct failed $entityId", e)
                            _entityStateMap.update { map -> map + (key to current) }
                        }
                }

                // ── Lock ──────────────────────────────────────────────────────
                is EntityAction.Lock -> {
                    current ?: return@launch
                    repo ?: return@launch
                    _entityStateMap.update { map ->
                        map + (key to current.copy(state = "locked"))
                    }
                    runCatching { repo.lockEntity(entityId) }
                        .onFailure { e ->
                            Log.e(TAG, "Lock failed $entityId", e)
                            _entityStateMap.update { map -> map + (key to current) }
                        }
                }
                is EntityAction.Unlock -> {
                    current ?: return@launch
                    repo ?: return@launch
                    _entityStateMap.update { map ->
                        map + (key to current.copy(state = "unlocked"))
                    }
                    runCatching { repo.unlockEntity(entityId) }
                        .onFailure { e ->
                            Log.e(TAG, "Unlock failed $entityId", e)
                            _entityStateMap.update { map -> map + (key to current) }
                        }
                }

                // ── Media player ──────────────────────────────────────────────
                is EntityAction.MediaPlayPause -> {
                    repo ?: return@launch
                    runCatching { repo.mediaPlayPause(entityId) }
                        .onFailure { e -> Log.e(TAG, "MediaPlayPause failed $entityId", e) }
                }
                is EntityAction.MediaPrevious -> {
                    repo ?: return@launch
                    runCatching { repo.mediaPreviousTrack(entityId) }
                        .onFailure { e -> Log.e(TAG, "MediaPrev failed $entityId", e) }
                }
                is EntityAction.MediaNext -> {
                    repo ?: return@launch
                    runCatching { repo.mediaNextTrack(entityId) }
                        .onFailure { e -> Log.e(TAG, "MediaNext failed $entityId", e) }
                }
                is EntityAction.SetMediaVolume -> {
                    current ?: return@launch
                    repo ?: return@launch
                    _entityStateMap.update { map ->
                        map + (key to current.copy(
                            attributes = (current.attributes ?: emptyMap()) +
                                mapOf("volume_level" to action.volume.toDouble())
                        ))
                    }
                    runCatching { repo.setMediaVolume(entityId, action.volume) }
                        .onFailure { e ->
                            Log.e(TAG, "SetVolume failed $entityId", e)
                            _entityStateMap.update { map -> map + (key to current) }
                        }
                }

                // ── Scene / Script (no optimistic update) ─────────────────────
                is EntityAction.Activate -> {
                    repo ?: return@launch
                    val domain = entityId.substringBefore(".")
                    runCatching { repo.callService(domain, "turn_on", entityId) }
                        .onFailure { e -> Log.e(TAG, "Activate failed $entityId", e) }
                }

                // ── Input number ──────────────────────────────────────────────
                is EntityAction.SetInputNumber -> {
                    current ?: return@launch
                    repo ?: return@launch
                    _entityStateMap.update { map ->
                        map + (key to current.copy(state = action.value.toString()))
                    }
                    runCatching { repo.setInputNumber(entityId, action.value) }
                        .onFailure { e ->
                            Log.e(TAG, "SetInputNumber failed $entityId", e)
                            _entityStateMap.update { map -> map + (key to current) }
                        }
                }
            }
        }
    }

    companion object {
        private const val TAG = "DashboardViewModel"

        private fun aggregateStates(states: Collection<ConnectionState>): ConnectionState = when {
            states.isEmpty() -> ConnectionState.Disconnected
            states.all { it is ConnectionState.Connected } -> ConnectionState.Connected
            states.any { it is ConnectionState.AuthFailed } -> ConnectionState.AuthFailed
            states.any { it is ConnectionState.Connecting } -> ConnectionState.Connecting
            else -> ConnectionState.Disconnected
        }
    }
}
