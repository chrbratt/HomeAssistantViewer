package se.inix.homeassistantviewer.ui.common

/** Maps a Home Assistant domain (e.g. `binary_sensor`) to a human label (`Binary Sensors`). */
fun domainDisplayName(domain: String): String = when (domain) {
    "light" -> "Lights"
    "switch" -> "Switches"
    "sensor" -> "Sensors"
    "binary_sensor" -> "Binary Sensors"
    "climate" -> "Climate"
    "cover" -> "Covers"
    "fan" -> "Fans"
    "lock" -> "Locks"
    "media_player" -> "Media Players"
    "input_boolean" -> "Input Booleans"
    "input_number" -> "Input Numbers"
    "automation" -> "Automations"
    "scene" -> "Scenes"
    "script" -> "Scripts"
    "weather" -> "Weather"
    "camera" -> "Cameras"
    "person" -> "Persons"
    "device_tracker" -> "Device Trackers"
    else -> domain.replace("_", " ").replaceFirstChar { it.uppercase() }
}
