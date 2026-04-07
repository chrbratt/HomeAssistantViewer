package se.inix.homeassistantviewer.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import se.inix.homeassistantviewer.BuildConfig
import se.inix.homeassistantviewer.data.model.HaEntityState
import se.inix.homeassistantviewer.data.model.LightControlBody
import se.inix.homeassistantviewer.data.model.ServiceCallBody

class HomeAssistantRepository(baseUrl: String, token: String) {

    private val api: HomeAssistantApi

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(DynamicUrlInterceptor(baseUrl, token))
            .addInterceptor(loggingInterceptor)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        api = Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(HomeAssistantApi::class.java)
    }

    /** Fetches all entities — used only by EntityPickerScreen on demand. */
    suspend fun getStates(): List<HaEntityState> = api.getStates()

    /**
     * Fetches only the given entity IDs in parallel.
     * Entities that fail to load are silently skipped.
     */
    suspend fun getStatesForEntities(entityIds: Set<String>): List<HaEntityState> {
        if (entityIds.isEmpty()) return emptyList()
        return coroutineScope {
            entityIds.map { entityId ->
                async { runCatching { api.getState(entityId) }.getOrNull() }
            }.awaitAll().filterNotNull()
        }
    }

    suspend fun callService(domain: String, service: String, entityId: String): List<HaEntityState> =
        api.callService(domain, service, ServiceCallBody(entityId))

    /** Turns on the light at the given brightness percentage (0–100). */
    suspend fun setLightBrightness(entityId: String, brightnessPct: Int): List<HaEntityState> =
        api.turnOnLight(LightControlBody(entityId, brightnessPct.coerceIn(0, 100)))

    suspend fun setCoverPosition(entityId: String, position: Int): List<HaEntityState> =
        api.callServiceWithData("cover", "set_cover_position",
            mapOf("entity_id" to entityId, "position" to position.coerceIn(0, 100)))

    suspend fun setClimateTemperature(entityId: String, temperature: Double): List<HaEntityState> =
        api.callServiceWithData("climate", "set_temperature",
            mapOf("entity_id" to entityId, "temperature" to temperature))

    suspend fun setClimateHvacMode(entityId: String, mode: String): List<HaEntityState> =
        api.callServiceWithData("climate", "set_hvac_mode",
            mapOf("entity_id" to entityId, "hvac_mode" to mode))

    suspend fun setFanPercentage(entityId: String, percentage: Int): List<HaEntityState> =
        api.callServiceWithData("fan", "set_percentage",
            mapOf("entity_id" to entityId, "percentage" to percentage.coerceIn(0, 100)))

    suspend fun lockEntity(entityId: String): List<HaEntityState> =
        api.callServiceWithData("lock", "lock", mapOf("entity_id" to entityId))

    suspend fun unlockEntity(entityId: String): List<HaEntityState> =
        api.callServiceWithData("lock", "unlock", mapOf("entity_id" to entityId))

    suspend fun mediaPlayPause(entityId: String): List<HaEntityState> =
        api.callServiceWithData("media_player", "media_play_pause",
            mapOf("entity_id" to entityId))

    suspend fun mediaPreviousTrack(entityId: String): List<HaEntityState> =
        api.callServiceWithData("media_player", "media_previous_track",
            mapOf("entity_id" to entityId))

    suspend fun mediaNextTrack(entityId: String): List<HaEntityState> =
        api.callServiceWithData("media_player", "media_next_track",
            mapOf("entity_id" to entityId))

    suspend fun setMediaVolume(entityId: String, volume: Float): List<HaEntityState> =
        api.callServiceWithData("media_player", "volume_set",
            mapOf("entity_id" to entityId, "volume_level" to volume.coerceIn(0f, 1f).toDouble()))

    suspend fun setInputNumber(entityId: String, value: Double): List<HaEntityState> =
        api.callServiceWithData("input_number", "set_value",
            mapOf("entity_id" to entityId, "value" to value))
}
