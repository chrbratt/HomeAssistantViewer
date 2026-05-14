package se.inix.homeassistantviewer.domain.entity

import se.inix.homeassistantviewer.data.model.HaEntityState

val HaEntityState.mediaTitle: String?
    get() = attributes?.get("media_title") as? String

val HaEntityState.mediaArtist: String?
    get() = attributes?.get("media_artist") as? String

/** Volume level 0.0–1.0, or null if not reported. */
val HaEntityState.volumeLevel: Float?
    get() = (attributes?.get("volume_level") as? Double)?.toFloat()
