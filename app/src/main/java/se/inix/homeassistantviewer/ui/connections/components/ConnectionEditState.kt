package se.inix.homeassistantviewer.ui.connections.components

/** UI state for the edit/add dialog. [id] = null means "this is a new connection". */
internal data class ConnectionEditState(
    val id: String? = null,
    val name: String = "",
    val baseUrl: String = "",
    val token: String = ""
)
