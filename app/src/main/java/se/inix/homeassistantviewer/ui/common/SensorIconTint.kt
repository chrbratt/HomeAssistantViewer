package se.inix.homeassistantviewer.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import se.inix.homeassistantviewer.data.model.HaEntityState

/**
 * Tint for the leading icon on a sensor card. Temperature gets a gradient cold→hot;
 * other device classes get a fixed semantic colour. Falls back to the theme primary.
 */
@Composable
fun sensorIconTint(entity: HaEntityState): Color {
    val dc = entity.attributes?.get("device_class") as? String
    val numericValue = entity.state.toDoubleOrNull()

    if (dc == "temperature" && numericValue != null) {
        return when {
            numericValue < 0 -> Color(0xFF90CAF9)
            numericValue < 8 -> Color(0xFF80DEEA)
            numericValue < 18 -> Color(0xFFA5D6A7)
            numericValue < 26 -> Color(0xFFFFCC80)
            else -> Color(0xFFEF9A9A)
        }
    }

    return when (dc) {
        "humidity", "moisture" -> Color(0xFF81D4FA)
        "wind_speed", "wind_direction" -> Color(0xFF80CBC4)
        "illuminance" -> Color(0xFFFFF176)
        "battery" -> Color(0xFFA5D6A7)
        "power", "energy", "voltage", "current" -> Color(0xFFFFCC80)
        "motion", "occupancy", "presence" ->
            if (entity.state == "on") Color(0xFFFFB74D) else Color(0xFF9E9E9E)
        else -> MaterialTheme.colorScheme.primary
    }
}
