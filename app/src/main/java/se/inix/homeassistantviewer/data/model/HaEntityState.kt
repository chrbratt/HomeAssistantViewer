package se.inix.homeassistantviewer.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlin.math.roundToInt

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

    /**
     * True when this light entity supports brightness control.
     * Checks `supported_color_modes`; if absent, falls back to whether a `brightness`
     * attribute is present. "onoff"-only lights do not support dimming.
     */
    val supportsBrightness: Boolean
        get() {
            if (domain != "light") return false
            val colorModes = attributes?.get("supported_color_modes") as? List<*>
            return colorModes?.any { it != "onoff" }
                ?: (attributes?.containsKey("brightness") == true)
        }

    /**
     * Current brightness as a percentage (0–100), or null if unavailable.
     * HA reports raw brightness as 0–255; Moshi deserialises numeric `Any` as Double.
     */
    val brightnessPercent: Int?
        get() {
            val raw = attributes?.get("brightness") as? Double ?: return null
            return (raw / 255.0 * 100.0).roundToInt().coerceIn(0, 100)
        }
}
