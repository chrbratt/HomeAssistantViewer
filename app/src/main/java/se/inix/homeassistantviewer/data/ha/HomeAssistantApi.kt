package se.inix.homeassistantviewer.data.ha

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import se.inix.homeassistantviewer.data.model.HaEntityState
import se.inix.homeassistantviewer.data.model.HaHistoryRow
import se.inix.homeassistantviewer.data.model.LightControlBody
import se.inix.homeassistantviewer.data.model.ServiceCallBody

interface HomeAssistantApi {

    @GET("api/states")
    suspend fun getStates(): List<HaEntityState>

    @GET("api/states/{entityId}")
    suspend fun getState(
        @Path("entityId") entityId: String
    ): HaEntityState

    @POST("api/services/{domain}/{service}")
    suspend fun callService(
        @Path("domain") domain: String,
        @Path("service") service: String,
        @Body body: ServiceCallBody
    ): List<HaEntityState>

    /** Generic service call with arbitrary extra data (climate, cover, fan, media_player, …). */
    @POST("api/services/{domain}/{service}")
    suspend fun callServiceWithData(
        @Path("domain") domain: String,
        @Path("service") service: String,
        @Body data: Map<String, @JvmSuppressWildcards Any>
    ): List<HaEntityState>

    @POST("api/services/light/turn_on")
    suspend fun turnOnLight(@Body body: LightControlBody): List<HaEntityState>

    /**
     * Home Assistant history endpoint. Returns a list-of-lists where the outer
     * list is one entry per filtered entity (always one in our case) and the
     * inner list is the chronological state timeline.
     *
     * `minimal_response=true` + `no_attributes=true` collapses every row to
     * `{state, last_changed}`, which is all the chart needs and keeps payloads
     * small enough for a 7-day window on mobile data.
     */
    @GET("api/history/period/{start}")
    suspend fun getHistory(
        @Path("start") startIso: String,
        @Query("filter_entity_id") entityId: String,
        @Query("end_time") endIso: String,
        @Query("minimal_response") minimal: Boolean = true,
        @Query("no_attributes") noAttributes: Boolean = true
    ): List<List<HaHistoryRow>>
}
