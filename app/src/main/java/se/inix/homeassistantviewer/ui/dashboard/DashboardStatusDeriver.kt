package se.inix.homeassistantviewer.ui.dashboard

import se.inix.homeassistantviewer.data.model.HaConnection
import se.inix.homeassistantviewer.data.ws.ConnectionState

/**
 * Pure function deriving the user-visible status banner from raw connection
 * health + in-flight REST refreshes.
 *
 * Worst-case wins: auth failures are surfaced first (they're permanent until
 * the user acts), then any "no connection at all", then ongoing refreshes,
 * then partial outages. Returns [DashboardStatusBar.Hidden] only when every
 * configured server is connected AND nothing is currently being fetched.
 */
internal fun deriveStatusBar(
    configured: List<HaConnection>,
    perConnectionState: Map<String, ConnectionState>,
    fetching: Set<String>
): DashboardStatusBar {
    if (configured.isEmpty()) return DashboardStatusBar.Hidden

    val states: List<ConnectionState?> = configured.map { perConnectionState[it.id] }
    val authFailed = states.count { it is ConnectionState.AuthFailed }
    val connected = states.count { it is ConnectionState.Connected }
    val connecting = states.count { it is ConnectionState.Connecting }
    val unobserved = states.count { it == null }
    val total = states.size
    // A server is only *actually* offline once we've observed it in Disconnected
    // state. Anything still Connecting or not yet observed is "in progress" —
    // calling it offline would be a lie before it even had a chance to fail.
    val confirmedOffline = total - authFailed - connected - connecting - unobserved
    val inProgress = connecting + unobserved

    if (authFailed > 0) {
        val text = if (total == 1) "Authentication failed — fix the token in Settings"
                   else "Authentication failed for $authFailed of $total servers"
        return DashboardStatusBar.Error(text)
    }

    if (connected == 0) {
        val text = when {
            // No state observed yet → first cold start; we haven't even tried.
            // Saying "Reconnecting" would imply we were once connected.
            unobserved == total -> "Connecting…"
            confirmedOffline == 0 -> "Connecting…"
            else -> "Reconnecting…"
        }
        return DashboardStatusBar.Connecting(text)
    }

    // From here: at least one server is Connected.

    // If others are still trying (or haven't been observed yet) keep saying
    // "connecting" — we don't know yet whether they'll succeed or fail.
    if (inProgress > 0 && confirmedOffline == 0) {
        val text = if (total == 2) "Connecting to remaining server…"
                   else "Connecting to $inProgress of $total servers…"
        return DashboardStatusBar.Refreshing(text)
    }

    if (fetching.isNotEmpty()) {
        return DashboardStatusBar.Refreshing("Loading latest values…")
    }

    if (confirmedOffline > 0) {
        // Some have definitively failed; others may still be in progress, but
        // the "offline" claim is now backed by an observed Disconnected state.
        return DashboardStatusBar.Warning(
            "$confirmedOffline of $total servers offline — showing last known values"
        )
    }

    return DashboardStatusBar.Hidden
}
