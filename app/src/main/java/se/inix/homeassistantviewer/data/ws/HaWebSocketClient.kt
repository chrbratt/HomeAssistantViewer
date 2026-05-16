package se.inix.homeassistantviewer.data.ws

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import se.inix.homeassistantviewer.BuildConfig
import se.inix.homeassistantviewer.data.model.HaEntityState
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages a persistent WebSocket connection to one Home Assistant installation.
 *
 * Lifecycle:
 * - constructed with fixed [baseUrl] and [token] (the [ConnectionPool] replaces the
 *   instance when credentials change)
 * - exposes [stateChanges] for incoming HA `state_changed` events
 * - exposes [connectionState] for the UI to display connection health
 * - call [disconnect] to permanently tear down; after that no more reconnects happen
 *
 * The OkHttpClient is supplied externally (shared at the AppContainer level) so
 * disposing a client does not leak the underlying thread / connection pool.
 */
class HaWebSocketClient(
    val connectionId: String,
    val baseUrl: String,
    val token: String,
    private val httpClient: OkHttpClient
) {
    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(supervisor + Dispatchers.IO)

    private val entityStateAdapter: JsonAdapter<HaEntityState> = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(HaEntityState::class.java)

    @Volatile private var disposed = false
    private var webSocket: WebSocket? = null
    private val messageId = AtomicInteger(1)
    private var reconnectJob: Job? = null
    private var backoffMs = INITIAL_BACKOFF_MS

    private val _stateChanges = MutableSharedFlow<HaEntityState>(extraBufferCapacity = 128)
    val stateChanges: SharedFlow<HaEntityState> = _stateChanges.asSharedFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val listener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) = handleMessage(webSocket, text)
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (disposed) return
            Log.w(TAG, "[$connectionId] Failure: ${t.message}")
            scheduleReconnect()
        }
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (BuildConfig.DEBUG) Log.d(TAG, "[$connectionId] Closed: $code $reason")
            if (disposed) return
            if (code != CLOSE_NORMAL) scheduleReconnect()
        }
    }

    init {
        if (baseUrl.isNotBlank() && token.isNotBlank()) connect()
    }

    /**
     * Forces an immediate reconnect attempt if not currently Connected.
     * Used when the app returns to foreground after being killed/paused.
     */
    fun ensureConnected() {
        if (disposed) return
        when (_connectionState.value) {
            ConnectionState.Connected -> Unit
            ConnectionState.AuthFailed -> Unit
            else -> {
                reconnectJob?.cancel()
                reconnectJob = null
                backoffMs = INITIAL_BACKOFF_MS
                connect()
            }
        }
    }

    fun disconnect() {
        disposed = true
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(CLOSE_NORMAL, "disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
        scope.cancel()
    }

    /**
     * Soft close — drops the socket but keeps the client reusable. Pending
     * reconnect jobs are cancelled and backoff is reset so the next
     * [ensureConnected] call retries immediately rather than waiting out the
     * previous backoff window.
     *
     * Called when the app is backgrounded (via [ConnectionPool.disconnectAll])
     * so the device stops parsing `state_changed` events the user cannot see.
     * The matching foreground `ON_START` reconnects via [ensureConnected].
     */
    fun closeSocket() {
        if (disposed) return
        reconnectJob?.cancel()
        reconnectJob = null
        backoffMs = INITIAL_BACKOFF_MS
        webSocket?.close(CLOSE_NORMAL, "background")
        webSocket = null
        // Preserve AuthFailed so reopening doesn't silently retry a bad token.
        if (_connectionState.value !is ConnectionState.AuthFailed) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    private fun toWsUrl(base: String): String {
        val trimmed = base.trimEnd('/')
        val wsBase = when {
            trimmed.startsWith("https://") -> "wss://" + trimmed.removePrefix("https://")
            trimmed.startsWith("http://") -> "ws://" + trimmed.removePrefix("http://")
            else -> "ws://$trimmed"
        }
        return "$wsBase/api/websocket"
    }

    private fun connect() {
        if (disposed) return
        if (BuildConfig.DEBUG) Log.d(TAG, "[$connectionId] Connecting to ${toWsUrl(baseUrl)}")
        _connectionState.value = ConnectionState.Connecting
        webSocket = httpClient.newWebSocket(
            Request.Builder().url(toWsUrl(baseUrl)).build(),
            listener
        )
    }

    private fun scheduleReconnect() {
        if (disposed) return
        if (_connectionState.value is ConnectionState.AuthFailed) return
        _connectionState.value = ConnectionState.Disconnected
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            if (BuildConfig.DEBUG) Log.d(TAG, "[$connectionId] Reconnecting in ${backoffMs}ms")
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            connect()
        }
    }

    private fun handleMessage(socket: WebSocket, text: String) {
        try {
            val json = JSONObject(text)
            when (json.optString("type")) {
                "auth_required" -> socket.send(
                    JSONObject().put("type", "auth").put("access_token", token).toString()
                )
                "auth_ok" -> {
                    if (BuildConfig.DEBUG) Log.d(TAG, "[$connectionId] Auth ok — subscribing")
                    backoffMs = INITIAL_BACKOFF_MS
                    _connectionState.value = ConnectionState.Connected
                    socket.send(
                        JSONObject()
                            .put("id", messageId.getAndIncrement())
                            .put("type", "subscribe_events")
                            .put("event_type", "state_changed")
                            .toString()
                    )
                }
                "auth_invalid" -> {
                    Log.e(TAG, "[$connectionId] Auth invalid")
                    _connectionState.value = ConnectionState.AuthFailed
                    socket.close(CLOSE_NORMAL, "auth_invalid")
                }
                "event" -> handleEvent(json)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$connectionId] Message handling error", e)
        }
    }

    private fun handleEvent(json: JSONObject) {
        val event = json.optJSONObject("event") ?: return
        if (event.optString("event_type") != "state_changed") return
        val newStateJson = event.optJSONObject("data")?.optJSONObject("new_state") ?: return
        val entityState = entityStateAdapter.fromJson(newStateJson.toString()) ?: return
        _stateChanges.tryEmit(entityState)
    }

    companion object {
        private const val TAG = "HaWebSocketClient"
        private const val INITIAL_BACKOFF_MS = 2_000L
        private const val MAX_BACKOFF_MS = 60_000L
        private const val CLOSE_NORMAL = 1000
    }
}
