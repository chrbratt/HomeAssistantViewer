package se.inix.homeassistantviewer.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HaConnection(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "baseUrl") val baseUrl: String,
    @Json(name = "token") val token: String
)
