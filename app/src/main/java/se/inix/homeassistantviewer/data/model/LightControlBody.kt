package se.inix.homeassistantviewer.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LightControlBody(
    @param:Json(name = "entity_id") val entityId: String,
    @param:Json(name = "brightness_pct") val brightnessPct: Int
)
