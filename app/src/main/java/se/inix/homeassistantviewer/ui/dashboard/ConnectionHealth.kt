package se.inix.homeassistantviewer.ui.dashboard

import androidx.compose.ui.graphics.Color

/**
 * Three-state health summary derived from [DashboardStatusBar]. Used by
 * the persistent Settings-icon badge so the user can confirm "everything is
 * up to date" even after the transient status banner has folded away.
 *
 * Mapping is intentionally simpler than the banner: the badge is a glance
 * cue, the banner already carries the full explanation.
 */
internal enum class ConnectionHealth(val dotColor: Color, val description: String) {
    /** Every configured connection is authenticated and values are fresh. */
    Healthy(Color(0xFF22C55E), "all connections healthy and up to date"),

    /** Initial WS handshake or REST refresh in flight. Transient. */
    Pending(Color(0xFFF59E0B), "connecting or refreshing"),

    /** At least one connection is offline, has failed auth, or errored out. */
    Unhealthy(Color(0xFFEF4444), "connection problem — open Settings");
}

/**
 * Pure projection — keeping the mapping next to the enum so the badge
 * composable stays a thin renderer with no business logic.
 */
internal fun DashboardStatusBar.toConnectionHealth(): ConnectionHealth = when (this) {
    is DashboardStatusBar.Hidden,
    is DashboardStatusBar.Ready -> ConnectionHealth.Healthy
    is DashboardStatusBar.Connecting,
    is DashboardStatusBar.Refreshing -> ConnectionHealth.Pending
    is DashboardStatusBar.Warning,
    is DashboardStatusBar.Error -> ConnectionHealth.Unhealthy
}
