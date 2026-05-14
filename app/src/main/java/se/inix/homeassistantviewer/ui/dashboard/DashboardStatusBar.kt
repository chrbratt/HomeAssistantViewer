package se.inix.homeassistantviewer.ui.dashboard

/**
 * User-visible status bar shown at the top of the dashboard. Replaces the
 * old green/red dot — the bar stays visible until every configured connection
 * is authenticated AND a fresh snapshot has been loaded.
 */
sealed class DashboardStatusBar {
    /** Everything is fine — values on screen are up-to-date. Hide the bar. */
    data object Hidden : DashboardStatusBar()
    /** Initial / blank state, before WS has reported anything. */
    data class Connecting(val text: String) : DashboardStatusBar()
    /** WS is connected; we are loading fresh values from REST. */
    data class Refreshing(val text: String) : DashboardStatusBar()
    /**
     * Transient "all clear" pulse shown briefly when transitioning from a progress
     * state to [Hidden]. Exists purely so the user actually sees confirmation that
     * data is up-to-date on fast networks — without it the bar would flash invisible.
     */
    data class Ready(val text: String) : DashboardStatusBar()
    /** Some — but not all — connections are unhealthy. */
    data class Warning(val text: String) : DashboardStatusBar()
    /** Hard error: every connection is failing or auth has been rejected. */
    data class Error(val text: String) : DashboardStatusBar()
}
