package se.inix.homeassistantviewer.data.model

import com.squareup.moshi.Json

/**
 * One row in Home Assistant's `/api/history/period/` response. With
 * `minimal_response=true` and `no_attributes=true` the server returns
 * only the state value and timestamp, keeping payloads small over the
 * mobile network.
 *
 * Both fields are nullable on purpose:
 *  - `state` may be the literal strings `"unknown"`/`"unavailable"`,
 *    or — observed in real HA installations — the JSON value `null`
 *    when recorder logged a missing/transition state. Treating either
 *    as the same "no value" gap downstream keeps the chart pipeline
 *    robust against new recorder behaviours.
 *  - `last_changed` is virtually always present; making it nullable
 *    just hardens parsing against future API quirks. Rows without a
 *    timestamp are dropped during series building because they have
 *    no X-coordinate to plot.
 */
data class HaHistoryRow(
    @param:Json(name = "state") val state: String?,
    @param:Json(name = "last_changed") val lastChanged: String?
)
