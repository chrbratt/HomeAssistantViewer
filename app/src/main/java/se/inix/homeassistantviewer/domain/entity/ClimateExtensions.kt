package se.inix.homeassistantviewer.domain.entity

import se.inix.homeassistantviewer.data.model.HaEntityState

val HaEntityState.currentTemperature: Double?
    get() = attributes?.get("current_temperature") as? Double

val HaEntityState.targetTemperature: Double?
    get() = attributes?.get("temperature") as? Double

/** HVAC modes supported by this climate device, e.g. `["off","heat","cool","auto"]`. */
val HaEntityState.hvacModes: List<String>
    get() = (attributes?.get("hvac_modes") as? List<*>)
        ?.filterIsInstance<String>() ?: emptyList()
