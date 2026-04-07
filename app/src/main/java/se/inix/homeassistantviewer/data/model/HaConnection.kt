package se.inix.homeassistantviewer.data.model

data class HaConnection(
    val id: String,
    val name: String,
    val baseUrl: String,
    val token: String
)
