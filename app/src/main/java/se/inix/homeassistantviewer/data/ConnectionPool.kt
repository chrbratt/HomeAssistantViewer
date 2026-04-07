package se.inix.homeassistantviewer.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages one [HaWebSocketClient] + [HomeAssistantRepository] pair per configured connection.
 * Reacts to [SettingsRepository.connections] changes: creates new clients, tears down removed ones,
 * and replaces clients whose URL or token has changed.
 */
class ConnectionPool(
    private val settingsRepository: SettingsRepository,
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

                // Disconnect clients for removed or changed connections
                current.forEach { (id, pair) ->
                    val updated = incoming[id]
                    if (updated == null ||
                        updated.baseUrl != pair.wsClient.baseUrl ||
                        updated.token != pair.wsClient.token
                    ) {
                        pair.wsClient.disconnect()
                    }
                }

                // Build the new map, reusing unchanged clients
                _clients.value = incoming.mapValues { (id, conn) ->
                    val existing = current[id]
                    if (existing != null &&
                        existing.wsClient.baseUrl == conn.baseUrl &&
                        existing.wsClient.token == conn.token
                    ) {
                        existing
                    } else {
                        ClientPair(
                            wsClient = HaWebSocketClient(id, conn.baseUrl, conn.token),
                            repository = HomeAssistantRepository(conn.baseUrl, conn.token)
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
}
