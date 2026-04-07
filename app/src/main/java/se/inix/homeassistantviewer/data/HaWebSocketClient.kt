package se.inix.homeassistantviewer.data

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

sealed class ConnectionState {
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data object AuthFailed : ConnectionState()
    data object Disconnected : ConnectionState()
}

/**
 * Manages a persistent WebSocket connection to one Home Assistant installation.
 *
 * Constructed with fixed [baseUrl] and [token]; the [ConnectionPool] is responsible
 * for recreating this instance when credentials change.
 */
class HaWebSocketClient(
    val connectionId: String,
    val baseUrl: String,
    val token: String
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val okHttpClient = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val entityStateAdapter: JsonAdapter<HaEntityState> = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(HaEntityState::class.java)

    private var webSocket: WebSocket? = null
    private val messageId = AtomicInteger(1)
    private var reconnectJob: Job? = null
    private var backoffMs = INITIAL_BACKOFF_MS

    private val _stateChanges = MutableSharedFlow<HaEntityState>(extraBufferCapacity = 64)
    val stateChanges: SharedFlow<HaEntityState> = _stateChanges.asSharedFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Must be declared before init so it is non-null when connect() is called.
    private val listener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) = handleMessage(webSocket, text)
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "[$connectionId] Failure: ${t.message}")
            scheduleReconnect(t.message)
        }
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (BuildConfig.DEBUG) Log.d(TAG, "[$connectionId] Closed: $code $reason")
            if (code != CLOSE_NORMAL) scheduleReconnect(reason)
        }
    }

    init {
        if (baseUrl.isNotBlank() && token.isNotBlank()) connect()
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
        if (BuildConfig.DEBUG) Log.d(TAG, "[$connectionId] Connecting to ${toWsUrl(baseUrl)}")
        _connectionState.value = ConnectionState.Connecting
        webSocket = okHttpClient.newWebSocket(
            Request.Builder().url(toWsUrl(baseUrl)).build(),
            listener
        )
    }

    fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(CLOSE_NORMAL, "disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    private fun scheduleReconnect(reason: String? = null) {
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
        scope.launch { _stateChanges.emit(entityState) }
    }

    companion object {
        private const val TAG = "HaWebSocketClient"
        private const val INITIAL_BACKOFF_MS = 2_000L
        private const val MAX_BACKOFF_MS = 60_000L
        private const val CLOSE_NORMAL = 1000
    }
}
