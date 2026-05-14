package se.inix.homeassistantviewer.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.inix.homeassistantviewer.data.events.AppEvents
import se.inix.homeassistantviewer.data.model.FavoriteItem
import se.inix.homeassistantviewer.data.model.HaEntityState
import se.inix.homeassistantviewer.data.settings.SettingsRepository
import se.inix.homeassistantviewer.data.ws.ConnectionPool
import se.inix.homeassistantviewer.data.ws.ConnectionState

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data object NoConnections : DashboardUiState()
    data object NoFavorites : DashboardUiState()
    data class Success(val items: List<DashboardItem>) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

private sealed class InitState {
    data object Loading : InitState()
    data class Error(val message: String) : InitState()
    data object Ready : InitState()
}

/**
 * Holds the dashboard's reactive state. Responsibilities are intentionally narrow:
 *  - aggregates [EntityKey] → [HaEntityState] from REST snapshots + WS events
 *  - derives the [DashboardUiState] and [DashboardStatusBar] for the UI
 *  - delegates every actual mutation to [EntityActionDispatcher]
 *
 * Anything domain-specific (light/cover/climate quirks, optimistic logic) lives
 * in the dispatcher; status banner derivation lives in [DashboardStatusDeriver].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val connectionPool: ConnectionPool,
    private val settingsRepository: SettingsRepository,
    private val appEvents: AppEvents
) : ViewModel() {

    private val _entityStateMap = MutableStateFlow<Map<EntityKey, HaEntityState>>(emptyMap())
    private val _initState = MutableStateFlow<InitState>(InitState.Loading)
    private val _fetchingConnections = MutableStateFlow<Set<String>>(emptySet())
    private val _perConnectionState = MutableStateFlow<Map<String, ConnectionState>>(emptyMap())
    private val _isRefreshing = MutableStateFlow(false)

    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    val dashboardColumns: StateFlow<Int> = settingsRepository.dashboardColumns

    private val dispatcher = EntityActionDispatcher(
        connectionPool = connectionPool,
        readState = { key -> _entityStateMap.value[key] },
        optimistic = { key, value -> _entityStateMap.update { it + (key to value) } }
    )

    val uiState: StateFlow<DashboardUiState> = combine(
        _entityStateMap,
        settingsRepository.favorites,
        settingsRepository.connections,
        _initState
    ) { stateMap, favorites, connections, initState ->
        when {
            connections.isEmpty() -> DashboardUiState.NoConnections
            favorites.isEmpty() -> DashboardUiState.NoFavorites
            initState is InitState.Loading -> DashboardUiState.Loading
            initState is InitState.Error -> DashboardUiState.Error(initState.message)
            else -> DashboardUiState.Success(
                items = favorites.map { fav ->
                    when (fav) {
                        is FavoriteItem.Entity -> DashboardItem.Entity(
                            connectionId = fav.connectionId,
                            entityId = fav.entityId,
                            entity = stateMap[EntityKey(fav.connectionId, fav.entityId)]
                        )
                        is FavoriteItem.Divider -> DashboardItem.Divider(fav.id)
                    }
                }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState.Loading)

    val statusBar: StateFlow<DashboardStatusBar> = combine(
        _perConnectionState,
        _fetchingConnections,
        settingsRepository.connections
    ) { perConn, fetching, configured ->
        deriveStatusBar(
            configured = configured,
            perConnectionState = perConn,
            fetching = fetching
        )
    }
        .distinctUntilChanged()
        .withReadyPulse()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DashboardStatusBar.Connecting("Connecting…")
        )

    init {
        observePerConnectionState()
        observePoolReadiness()
        observePerConnectionReconnects()
        collectWebSocketUpdates()
        watchFavoritesChanges()
        observeForegroundEvents()
    }

    // ── Public actions ─────────────────────────────────────────────────────────

    fun fetchStates() = fetchInitialSnapshot()

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            runCatching { fetchStatesInternal() }
                .onSuccess { map -> if (map.isNotEmpty()) _entityStateMap.value = map }
                .onFailure { Log.w(TAG, "Pull-to-refresh failed", it) }
            _isRefreshing.value = false
        }
    }

    /**
     * Called when the process returns to foreground (via [AppEvents]). Forces every WS to
     * reconnect if needed and re-fetches a fresh snapshot — the user just came back to
     * the app and should not see stale numbers.
     */
    fun onAppForegrounded() {
        connectionPool.reconnectAll()
        viewModelScope.launch {
            runCatching { fetchStatesInternal() }
                .onSuccess { map -> if (map.isNotEmpty()) _entityStateMap.value = map }
                .onFailure { Log.w(TAG, "Foreground refresh failed", it) }
        }
    }

    fun saveItemOrder(ordered: List<DashboardItem>) {
        settingsRepository.saveFavoriteOrder(
            ordered.map { item ->
                when (item) {
                    is DashboardItem.Entity -> FavoriteItem.Entity(item.connectionId, item.entityId)
                    is DashboardItem.Divider -> FavoriteItem.Divider(item.id)
                }
            }
        )
    }

    fun removeItem(item: DashboardItem) {
        val target: FavoriteItem = when (item) {
            is DashboardItem.Entity -> FavoriteItem.Entity(item.connectionId, item.entityId)
            is DashboardItem.Divider -> FavoriteItem.Divider(item.id)
        }
        settingsRepository.removeFavorite(target)
    }

    fun addDivider() {
        settingsRepository.addDivider()
    }

    fun performAction(action: EntityAction) {
        viewModelScope.launch { dispatcher.dispatch(action) }
    }

    // ── Internal observers ─────────────────────────────────────────────────────

    private fun observeForegroundEvents() {
        viewModelScope.launch {
            appEvents.foregroundEvents.collect { onAppForegrounded() }
        }
    }

    private fun observePerConnectionState() {
        viewModelScope.launch {
            connectionPool.clients.collectLatest { clients ->
                if (clients.isEmpty()) {
                    _perConnectionState.value = emptyMap()
                    return@collectLatest
                }
                coroutineScope {
                    clients.forEach { (connId, pair) ->
                        launch {
                            pair.wsClient.connectionState.collect { state ->
                                _perConnectionState.update { it + (connId to state) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observePoolReadiness() {
        viewModelScope.launch {
            connectionPool.clients
                .map { it.keys }
                .distinctUntilChanged()
                .collect { keys ->
                    if (keys.isEmpty()) _initState.value = InitState.Ready
                    else fetchInitialSnapshot()
                }
        }
    }

    /**
     * Re-fetches one connection's snapshot whenever that specific WS transitions
     * back into Connected. Multi-connection setups recover independently — the
     * previous (now-removed) implementation only triggered when every connection
     * came back at the same time.
     */
    private fun observePerConnectionReconnects() {
        viewModelScope.launch {
            connectionPool.clients.collectLatest { clients ->
                if (clients.isEmpty()) return@collectLatest
                coroutineScope {
                    clients.forEach { (connId, pair) ->
                        launch {
                            var previous: ConnectionState? = null
                            pair.wsClient.connectionState.collect { state ->
                                val transitioned = state is ConnectionState.Connected &&
                                    previous != null && previous !is ConnectionState.Connected
                                previous = state
                                if (transitioned) refreshConnection(connId)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun refreshConnection(connectionId: String) {
        val favorites = settingsRepository.favorites.value
            .filterIsInstance<FavoriteItem.Entity>()
            .filter { it.connectionId == connectionId }
        if (favorites.isEmpty()) return
        val repo = connectionPool.repositoryFor(connectionId) ?: return

        _fetchingConnections.update { it + connectionId }
        runCatching {
            repo.getStatesForEntities(favorites.map { it.entityId }.toSet())
        }.onSuccess { states ->
            _entityStateMap.update { current ->
                current + states.associateBy { EntityKey(connectionId, it.entityId) }
            }
        }.onFailure { e ->
            Log.w(TAG, "Per-connection refresh failed for $connectionId", e)
        }
        _fetchingConnections.update { it - connectionId }
    }

    private suspend fun fetchStatesInternal(): Map<EntityKey, HaEntityState> {
        val favorites = settingsRepository.favorites.value
            .filterIsInstance<FavoriteItem.Entity>()
        if (favorites.isEmpty()) return emptyMap()
        return coroutineScope {
            val fetches = favorites.groupBy { it.connectionId }
                .map { (connId, favs) ->
                    async {
                        val repo = connectionPool.repositoryFor(connId)
                            ?: return@async ConnectionFetch(connId, success = false, results = emptyList())
                        _fetchingConnections.update { it + connId }
                        val result = runCatching {
                            repo.getStatesForEntities(favs.map { it.entityId }.toSet())
                        }
                        _fetchingConnections.update { it - connId }
                        result.fold(
                            onSuccess = { states ->
                                ConnectionFetch(connId, success = true,
                                    results = states.map { EntityKey(connId, it.entityId) to it })
                            },
                            onFailure = { e ->
                                Log.w(TAG, "Snapshot failed for connection $connId", e)
                                ConnectionFetch(connId, success = false, results = emptyList())
                            }
                        )
                    }
                }.awaitAll()

            val allFailed = fetches.isNotEmpty() && fetches.none { it.success }
            if (allFailed) {
                _initState.value = InitState.Error(
                    "Could not reach any Home Assistant server. " +
                        "Check that the URL and token are correct."
                )
            }
            fetches.flatMap { it.results }.toMap()
        }
    }

    private fun fetchInitialSnapshot() {
        viewModelScope.launch {
            val favorites = settingsRepository.favorites.value
                .filterIsInstance<FavoriteItem.Entity>()
            if (favorites.isEmpty()) {
                _initState.value = InitState.Ready
                return@launch
            }
            _initState.value = InitState.Loading
            runCatching { fetchStatesInternal() }.fold(
                onSuccess = { map ->
                    _entityStateMap.value = map
                    if (_initState.value !is InitState.Error) _initState.value = InitState.Ready
                },
                onFailure = { e ->
                    Log.e(TAG, "All snapshots failed", e)
                    _initState.value = InitState.Error(e.message ?: "Failed to load dashboard")
                }
            )
        }
    }

    private fun collectWebSocketUpdates() {
        viewModelScope.launch {
            connectionPool.clients.collectLatest { clients ->
                coroutineScope {
                    clients.forEach { (connectionId, pair) ->
                        launch {
                            pair.wsClient.stateChanges.collect { entity ->
                                val key = EntityKey(connectionId, entity.entityId)
                                val stillFavorite = settingsRepository.favorites.value
                                    .filterIsInstance<FavoriteItem.Entity>()
                                    .any { it.connectionId == connectionId && it.entityId == entity.entityId }
                                if (stillFavorite) {
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
     * Refetches when favorites are added/removed so newly added entities show
     * a real value instead of an "Unavailable" card until the next state push.
     */
    private fun watchFavoritesChanges() {
        viewModelScope.launch {
            var previous: List<FavoriteItem> = emptyList()
            settingsRepository.favorites.collect { current ->
                val newEntities = current
                    .filterIsInstance<FavoriteItem.Entity>().toSet() -
                    previous.filterIsInstance<FavoriteItem.Entity>().toSet()
                previous = current
                if (newEntities.isEmpty()) return@collect

                runCatching {
                    coroutineScope {
                        newEntities.groupBy { it.connectionId }.map { (connId, favs) ->
                            async {
                                val repo = connectionPool.repositoryFor(connId)
                                    ?: return@async emptyList<Pair<EntityKey, HaEntityState>>()
                                _fetchingConnections.update { it + connId }
                                val result = runCatching {
                                    repo.getStatesForEntities(favs.map { it.entityId }.toSet())
                                        .map { EntityKey(connId, it.entityId) to it }
                                }.getOrElse { emptyList() }
                                _fetchingConnections.update { it - connId }
                                result
                            }
                        }.awaitAll().flatten().toMap()
                    }
                }.onSuccess { map ->
                    if (map.isNotEmpty()) _entityStateMap.update { it + map }
                    if (_initState.value !is InitState.Error) _initState.value = InitState.Ready
                }.onFailure { e ->
                    Log.w(TAG, "Favorites refresh failed", e)
                }
            }
        }
    }

    private data class ConnectionFetch(
        val connectionId: String,
        val success: Boolean,
        val results: List<Pair<EntityKey, HaEntityState>>
    )

    /**
     * Inserts a short [DashboardStatusBar.Ready] pulse whenever the status
     * transitions from a progress state (Connecting/Refreshing) to Hidden.
     * Without this, fast networks would hide the banner before the user
     * notices, leaving them with no visual confirmation that values are fresh.
     *
     * Uses [transformLatest] so the pulse is interruptible: any new upstream
     * emission cancels the delay and surfaces the new state immediately, so
     * errors are never masked. Plain `flow { collectLatest { emit … } }` is
     * not safe here — it violates Flow's same-coroutine emission invariant.
     */
    private fun Flow<DashboardStatusBar>.withReadyPulse(
        durationMs: Long = READY_PULSE_MS
    ): Flow<DashboardStatusBar> {
        var lastRaw: DashboardStatusBar = DashboardStatusBar.Connecting("Connecting…")
        return transformLatest { current ->
            val previous = lastRaw
            lastRaw = current
            val wasProgress = previous is DashboardStatusBar.Connecting ||
                previous is DashboardStatusBar.Refreshing
            if (current is DashboardStatusBar.Hidden && wasProgress) {
                emit(DashboardStatusBar.Ready("Connected — values are up to date"))
                delay(durationMs)
            }
            emit(current)
        }
    }

    companion object {
        private const val TAG = "DashboardViewModel"
        private const val READY_PULSE_MS = 1_500L
    }
}
