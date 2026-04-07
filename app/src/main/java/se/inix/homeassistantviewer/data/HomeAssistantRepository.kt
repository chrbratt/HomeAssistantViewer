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
}
