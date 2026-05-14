package se.inix.homeassistantviewer.domain.entity

import se.inix.homeassistantviewer.data.model.HaEntityState

/** Current value parsed from the state string. */
val HaEntityState.inputNumberValue: Double?
    get() = state.toDoubleOrNull()

val HaEntityState.inputNumberMin: Double
    get() = (attributes?.get("min") as? Double) ?: 0.0

val HaEntityState.inputNumberMax: Double
    get() = (attributes?.get("max") as? Double) ?: 100.0

val HaEntityState.inputNumberStep: Double
    get() = (attributes?.get("step") as? Double) ?: 1.0
