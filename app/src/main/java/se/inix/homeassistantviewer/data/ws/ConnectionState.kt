package se.inix.homeassistantviewer.data.ws

/** Live state of a single Home Assistant WebSocket connection. */
sealed class ConnectionState {
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    /** Server rejected our token — the user must fix it before reconnect helps. */
    data object AuthFailed : ConnectionState()
    data object Disconnected : ConnectionState()
}
