package se.inix.homeassistantviewer.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Raw Home Assistant entity state as returned by the REST API and the
 * `state_changed` WebSocket event.
 *
 * Domain-specific helpers (brightness, cover position, climate target, …)
 * live as extension properties in `se.inix.homeassistantviewer.domain.entity`
 * so this model stays a thin transport object.
 */
@JsonClass(generateAdapter = true)
data class HaEntityState(
    @param:Json(name = "entity_id") val entityId: String,
    @param:Json(name = "state") val state: String,
    @param:Json(name = "attributes") val attributes: Map<String, Any>?,
    @param:Json(name = "last_changed") val lastChanged: String,
    @param:Json(name = "last_updated") val lastUpdated: String
) {
    val domain: String get() = entityId.substringBefore(".")

    val friendlyName: String?
        get() = attributes?.get("friendly_name") as? String

    val unitOfMeasurement: String?
        get() = attributes?.get("unit_of_measurement") as? String
}
