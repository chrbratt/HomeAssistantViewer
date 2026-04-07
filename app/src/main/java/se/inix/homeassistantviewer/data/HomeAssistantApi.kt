package se.inix.homeassistantviewer.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import se.inix.homeassistantviewer.data.model.HaEntityState
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

    @POST("api/services/light/turn_on")
    suspend fun turnOnLight(@Body body: LightControlBody): List<HaEntityState>
}
