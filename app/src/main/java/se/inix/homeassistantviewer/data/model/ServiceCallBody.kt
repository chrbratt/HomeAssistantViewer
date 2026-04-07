package se.inix.homeassistantviewer.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ServiceCallBody(
    @param:Json(name = "entity_id") val entityId: String
)
