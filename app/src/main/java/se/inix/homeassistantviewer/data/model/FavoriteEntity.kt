package se.inix.homeassistantviewer.data.model

/**
 * Identifies a favorited entity and which connection it belongs to.
 * Stored as "connectionId|entityId" pairs.
 */
data class FavoriteEntity(
    val connectionId: String,
    val entityId: String
)
