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

    // ── Light ─────────────────────────────────────────────────────────────────

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

    // ── Cover ─────────────────────────────────────────────────────────────────

    /** Current cover position 0–100, or null if the device does not report one. */
    val coverPosition: Int?
        get() = (attributes?.get("current_position") as? Double)?.toInt()

    // ── Climate ───────────────────────────────────────────────────────────────

    val currentTemperature: Double?
        get() = attributes?.get("current_temperature") as? Double

    val targetTemperature: Double?
        get() = attributes?.get("temperature") as? Double

    /** HVAC modes supported by this climate device, e.g. ["off","heat","cool","auto"]. */
    val hvacModes: List<String>
        get() = (attributes?.get("hvac_modes") as? List<*>)
            ?.filterIsInstance<String>() ?: emptyList()

    // ── Fan ───────────────────────────────────────────────────────────────────

    /** Fan speed as a percentage 0–100, or null if not reported. */
    val fanPercentage: Int?
        get() = (attributes?.get("percentage") as? Double)?.toInt()

    // ── Media player ──────────────────────────────────────────────────────────

    val mediaTitle: String?
        get() = attributes?.get("media_title") as? String

    val mediaArtist: String?
        get() = attributes?.get("media_artist") as? String

    /** Volume level 0.0–1.0, or null if not reported. */
    val volumeLevel: Float?
        get() = (attributes?.get("volume_level") as? Double)?.toFloat()

    // ── Input number ──────────────────────────────────────────────────────────

    /** Current value parsed from the state string. */
    val inputNumberValue: Double?
        get() = state.toDoubleOrNull()

    val inputNumberMin: Double
        get() = (attributes?.get("min") as? Double) ?: 0.0

    val inputNumberMax: Double
        get() = (attributes?.get("max") as? Double) ?: 100.0

    val inputNumberStep: Double
        get() = (attributes?.get("step") as? Double) ?: 1.0
}
