package se.inix.homeassistantviewer.domain.entity

import se.inix.homeassistantviewer.data.model.HaEntityState

/** Current cover position 0–100, or null if the device does not report one. */
val HaEntityState.coverPosition: Int?
    get() = (attributes?.get("current_position") as? Double)?.toInt()
