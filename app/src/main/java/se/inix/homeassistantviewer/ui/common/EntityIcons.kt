package se.inix.homeassistantviewer.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Blinds
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Toys
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import se.inix.homeassistantviewer.data.model.HaEntityState

/**
 * Picks an icon for an entity based on its domain and `device_class`. Used by
 * cards and by the entity-picker domain headers.
 */
fun getIconForEntity(entity: HaEntityState): ImageVector {
    val dc = entity.attributes?.get("device_class") as? String
    return when {
        entity.domain == "light" -> Icons.Rounded.Lightbulb
        entity.domain == "switch" -> Icons.Rounded.Power
        entity.domain == "climate" -> Icons.Rounded.Thermostat
        entity.domain == "cover" -> Icons.Rounded.Blinds
        entity.domain == "fan" -> Icons.Rounded.Toys
        entity.domain == "lock" -> Icons.Rounded.Lock
        entity.domain == "media_player" -> Icons.Rounded.PlayArrow
        entity.domain == "weather" -> Icons.Rounded.WbCloudy
        dc == "temperature" -> Icons.Rounded.Thermostat
        dc == "humidity" || dc == "moisture" -> Icons.Rounded.WaterDrop
        dc == "wind_speed" || dc == "wind_direction" -> Icons.Rounded.Air
        dc == "illuminance" -> Icons.Rounded.WbSunny
        dc == "battery" -> Icons.Rounded.BatteryFull
        dc == "power" || dc == "energy" || dc == "voltage" || dc == "current" -> Icons.Rounded.Bolt
        dc == "motion" || dc == "occupancy" || dc == "presence" ->
            Icons.AutoMirrored.Rounded.DirectionsWalk
        else -> Icons.Rounded.Sensors
    }
}

/** Domain-only fallback when we don't yet have a full [HaEntityState]. */
fun getIconForDomain(domain: String): ImageVector = when (domain) {
    "light" -> Icons.Rounded.Lightbulb
    "switch" -> Icons.Rounded.Power
    "climate" -> Icons.Rounded.Thermostat
    "cover" -> Icons.Rounded.Blinds
    "fan" -> Icons.Rounded.Toys
    "lock" -> Icons.Rounded.Lock
    "media_player" -> Icons.Rounded.PlayArrow
    "weather" -> Icons.Rounded.WbCloudy
    "sensor", "binary_sensor" -> Icons.Rounded.Sensors
    else -> Icons.Rounded.Sensors
}
