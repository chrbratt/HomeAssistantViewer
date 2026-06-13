package se.inix.homeassistantviewer.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

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
    @param:Json(name = "last_updated") val lastUpdated: String,
    /**
     * When the entity last *reported* to HA — even if the value was
     * identical to the previous one. Newer than `last_updated`/`last_changed`
     * and absent on older HA cores (and some WS payloads), hence nullable.
     */
    @param:Json(name = "last_reported") val lastReported: String? = null
) {
    val domain: String get() = entityId.substringBefore(".")

    val friendlyName: String?
        get() = attributes?.get("friendly_name") as? String

    val unitOfMeasurement: String?
        get() = attributes?.get("unit_of_measurement") as? String

    /**
     * `false` when HA has no real reading for this entity right now
     * (`unavailable`/`unknown`). The dashboard uses this to keep showing the
     * last good value (dimmed) instead of replacing it with a blank card.
     */
    val hasUsableValue: Boolean
        get() = state != STATE_UNAVAILABLE && state != STATE_UNKNOWN

    val lastUpdatedInstant: Instant? get() = parseInstantOrNull(lastUpdated)

    val lastReportedInstant: Instant? get() = parseInstantOrNull(lastReported)

    companion object {
        const val STATE_UNAVAILABLE = "unavailable"
        const val STATE_UNKNOWN = "unknown"

        private fun parseInstantOrNull(value: String?): Instant? =
            value?.let { runCatching { Instant.parse(it) }.getOrNull() }
    }
}
