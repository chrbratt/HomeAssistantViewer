package se.inix.homeassistantviewer.domain.entity

import se.inix.homeassistantviewer.data.model.HaEntityState

/** Fan speed as a percentage 0–100, or null if not reported. */
val HaEntityState.fanPercentage: Int?
    get() = (attributes?.get("percentage") as? Double)?.toInt()
