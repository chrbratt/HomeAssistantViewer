package se.inix.homeassistantviewer.data.ws

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import se.inix.homeassistantviewer.data.ha.HomeAssistantRepository
import se.inix.homeassistantviewer.data.settings.SettingsRepository

/**
 * Manages one [HaWebSocketClient] + [HomeAssistantRepository] pair per configured connection.
 * Reacts to [SettingsRepository.connections] changes: creates new clients, tears down removed
 * ones, and replaces clients whose URL or token has changed.
 *
 * All clients derive from a shared OkHttpClient so disposing a per-connection client does not
 * leak the underlying thread / connection pool.
 */
class ConnectionPool(
    settingsRepository: SettingsRepository,
    private val sharedHttpClient: OkHttpClient,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    data class ClientPair(
        val wsClient: HaWebSocketClient,
        val repository: HomeAssistantRepository
    )

    private val _clients = MutableStateFlow<Map<String, ClientPair>>(emptyMap())
    val clients: StateFlow<Map<String, ClientPair>> = _clients.asStateFlow()

    init {
        scope.launch {
            settingsRepository.connections.collect { connections ->
                val incoming = connections.associateBy { it.id }
                val current = _clients.value

                current.forEach { (id, pair) ->
                    val updated = incoming[id]
                    if (updated == null ||
                        updated.baseUrl != pair.wsClient.baseUrl ||
                        updated.token != pair.wsClient.token
                    ) {
                        pair.wsClient.disconnect()
                    }
                }

                _clients.value = incoming.mapValues { (id, conn) ->
                    val existing = current[id]
                    if (existing != null &&
                        existing.wsClient.baseUrl == conn.baseUrl &&
                        existing.wsClient.token == conn.token
                    ) {
                        existing
                    } else {
                        ClientPair(
                            wsClient = HaWebSocketClient(id, conn.baseUrl, conn.token, sharedHttpClient),
                            repository = HomeAssistantRepository(conn.baseUrl, conn.token, sharedHttpClient)
                        )
                    }
                }
            }
        }
    }

    fun repositoryFor(connectionId: String): HomeAssistantRepository? =
        _clients.value[connectionId]?.repository

    fun wsClientFor(connectionId: String): HaWebSocketClient? =
        _clients.value[connectionId]?.wsClient

    /**
     * Forces every WS client to attempt a reconnect if not currently connected.
     * Called when the app returns to foreground after being paused or killed.
     */
    fun reconnectAll() {
        _clients.value.values.forEach { it.wsClient.ensureConnected() }
    }

    /**
     * Closes every active WebSocket. Called from [StugaApplication] after the
     * process has been backgrounded for a debounce window so the device does
     * not keep parsing `state_changed` events the user cannot see — saving
     * battery and mobile data on busy HA installations.
     *
     * The matching [reconnectAll] call on `ON_START` restores live updates.
     */
    fun disconnectAll() {
        _clients.value.values.forEach { it.wsClient.closeSocket() }
    }
}
