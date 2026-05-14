package se.inix.homeassistantviewer.domain.entity

import se.inix.homeassistantviewer.data.model.HaEntityState
import kotlin.math.roundToInt

/**
 * True when this light entity supports brightness control.
 *
 * Checks `supported_color_modes`; if absent, falls back to whether a
 * `brightness` attribute is present. "onoff"-only lights do not support
 * dimming.
 */
val HaEntityState.supportsBrightness: Boolean
    get() {
        if (domain != "light") return false
        val colorModes = attributes?.get("supported_color_modes") as? List<*>
        return colorModes?.any { it != "onoff" }
            ?: (attributes?.containsKey("brightness") == true)
    }

/**
 * Current brightness as a percentage (0–100), or null if unavailable.
 *
 * HA reports raw brightness as 0–255; Moshi deserialises numeric `Any` as Double.
 */
val HaEntityState.brightnessPercent: Int?
    get() {
        val raw = attributes?.get("brightness") as? Double ?: return null
        return (raw / 255.0 * 100.0).roundToInt().coerceIn(0, 100)
    }
