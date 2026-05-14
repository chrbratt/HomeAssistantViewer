package se.inix.homeassistantviewer.ui.common

/**
 * Cleans up a sensor state for display:
 *  - numeric values → integer when whole, one decimal otherwise
 *  - non-numeric strings → first letter capitalised
 */
fun formatSensorValue(state: String): String {
    val d = state.toDoubleOrNull() ?: return state.replaceFirstChar { it.uppercase() }
    return if (d % 1.0 == 0.0) d.toLong().toString() else "%.1f".format(d)
}
