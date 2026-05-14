package se.inix.homeassistantviewer.data.ha

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import se.inix.homeassistantviewer.BuildConfig
import se.inix.homeassistantviewer.data.settings.UrlNormaliser
import java.io.IOException

sealed class AddressProbeResult {
    data class Reachable(val httpCode: Int) : AddressProbeResult()
    data class Unreachable(val detail: String) : AddressProbeResult()
    data class InvalidInput(val detail: String) : AddressProbeResult()
}

sealed class ApiProbeResult {
    data object Ok : ApiProbeResult()
    data class HttpError(val code: Int) : ApiProbeResult()
    data class NetworkError(val detail: String) : ApiProbeResult()
    data class Skipped(val reason: String) : ApiProbeResult()
    data object InvalidInput : ApiProbeResult()
}

/**
 * Live connection diagnostics shown in the "Add connection" dialog. Calls do
 * not go through Retrofit so the user can verify URL + token before saving.
 */
class HomeAssistantConnectionTester(
    private val client: OkHttpClient
) {

    private val userAgent: String = "HomeAssistantViewer/${BuildConfig.VERSION_NAME}"

    suspend fun probeAddress(baseUrl: String): AddressProbeResult = withContext(Dispatchers.IO) {
        val normalised = UrlNormaliser.normalise(baseUrl)
            ?: return@withContext AddressProbeResult.InvalidInput(
                if (baseUrl.isBlank()) "Enter a base URL" else "Invalid URL"
            )
        val httpUrl = normalised.toHttpUrlOrNull()
            ?: return@withContext AddressProbeResult.InvalidInput("Invalid URL")
        val request = Request.Builder()
            .url(httpUrl)
            .get()
            .header("User-Agent", userAgent)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                AddressProbeResult.Reachable(httpCode = response.code)
            }
        } catch (e: IOException) {
            AddressProbeResult.Unreachable(e.message ?: e.javaClass.simpleName)
        }
    }

    suspend fun probeApi(baseUrl: String, token: String): ApiProbeResult = withContext(Dispatchers.IO) {
        val normalised = UrlNormaliser.normalise(baseUrl)
            ?: return@withContext ApiProbeResult.InvalidInput
        if (token.isBlank()) {
            return@withContext ApiProbeResult.Skipped("Add a token to test the API")
        }
        val apiUrl = mergeBaseWithPathSegments(normalised, listOf("api", "states"))
            ?: return@withContext ApiProbeResult.InvalidInput
        val request = Request.Builder()
            .url(apiUrl)
            .get()
            .header("Authorization", "Bearer ${token.trim()}")
            .header("Content-Type", "application/json")
            .header("User-Agent", userAgent)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) ApiProbeResult.Ok
                else ApiProbeResult.HttpError(code = response.code)
            }
        } catch (e: IOException) {
            ApiProbeResult.NetworkError(e.message ?: e.javaClass.simpleName)
        }
    }

    companion object {
        /**
         * Same path merge as [DynamicUrlInterceptor]: user base path + REST path segments.
         */
        private fun mergeBaseWithPathSegments(baseUrlString: String, pathSegments: List<String>): HttpUrl? {
            val newBase = baseUrlString.trim().toHttpUrlOrNull() ?: return null
            val builder = HttpUrl.Builder()
                .scheme(newBase.scheme)
                .host(newBase.host)
                .port(newBase.port)
            builder.encodedPath("/")
            for (segment in newBase.pathSegments) {
                if (segment.isNotEmpty()) builder.addPathSegment(segment)
            }
            for (segment in pathSegments) {
                if (segment.isNotEmpty()) builder.addPathSegment(segment)
            }
            return builder.build()
        }
    }
}
