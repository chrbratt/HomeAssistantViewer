package se.inix.homeassistantviewer.ui.dashboard

/** Compound key for the dashboard's entity state map (per-connection, per-entity). */
data class EntityKey(val connectionId: String, val entityId: String)
