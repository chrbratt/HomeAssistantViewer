package se.inix.homeassistantviewer.domain.history

/**
 * Decides how to render the history series for a given entity based on its
 * HA domain and unit-of-measurement.
 *
 * Pure / stateless — kept off the [HomeAssistantRepository] so it stays
 * testable with no Android dependencies.
 */
internal object SeriesClassifier {

    /**
     * Domains whose state is fundamentally on/off. Plotting numeric
     * interpolation for these would be misleading — we want a step chart.
     */
    private val binaryDomains = setOf(
        "switch", "light", "binary_sensor", "input_boolean", "lock", "fan"
    )

    fun classify(domain: String, unitOfMeasurement: String?): SeriesKind = when {
        domain in binaryDomains -> SeriesKind.Binary
        unitOfMeasurement != null -> SeriesKind.Numeric(unitOfMeasurement)
        // sensors without unit are usually enumerated text (e.g. weather state)
        domain == "sensor" -> SeriesKind.Discrete
        else -> SeriesKind.Discrete
    }

    /**
     * Projects a raw HA state string into a numeric value the chart can plot.
     * - Binary: on/locked/open/playing -> 1.0; off/unlocked/closed/idle -> 0.0
     * - Numeric: tries to parse as double; null on failure
     * - Discrete: always null (no useful y-coordinate)
     *
     * Unparseable values produce null so the chart can show them as gaps
     * rather than fake zeros (e.g. an "unavailable" sensor doesn't suddenly
     * drop to 0 °C).
     */
    fun project(kind: SeriesKind, rawState: String): Double? = when (kind) {
        is SeriesKind.Binary -> when (rawState.lowercase()) {
            "on", "open", "opening", "locked", "playing", "home", "true" -> 1.0
            "off", "closed", "closing", "unlocked", "idle", "paused",
            "not_home", "false" -> 0.0
            else -> null
        }
        is SeriesKind.Numeric -> rawState.toDoubleOrNull()
        SeriesKind.Discrete -> null
    }
}
