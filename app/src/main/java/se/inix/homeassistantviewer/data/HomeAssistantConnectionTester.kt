package se.inix.homeassistantviewer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class AddressProbeResult {
    data class Reachable(val httpCode: Int, val reasonPhrase: String) : AddressProbeResult()
    data class Unreachable(val detail: String) : AddressProbeResult()
    data class InvalidInput(val detail: String) : AddressProbeResult()
}

sealed class ApiProbeResult {
    data object Ok : ApiProbeResult()
    data class HttpError(
        val code: Int,
        val reasonPhrase: String,
        val bodySnippet: String?
    ) : ApiProbeResult()

    data class NetworkError(val detail: String) : ApiProbeResult()
    data class Skipped(val reason: String) : ApiProbeResult()
    data class InvalidInput(val detail: String) : ApiProbeResult()
}

class HomeAssistantConnectionTester(
    private val client: OkHttpClient = defaultClient()
) {

    suspend fun probeAddress(baseUrl: String): AddressProbeResult = withContext(Dispatchers.IO) {
        val trimmed = baseUrl.trim()
        if (trimmed.isEmpty()) {
            return@withContext AddressProbeResult.InvalidInput("Enter a base URL")
        }
        val httpUrl = trimmed.toHttpUrlOrNull()
            ?: return@withContext AddressProbeResult.InvalidInput("Invalid URL")
        val request = Request.Builder()
            .url(httpUrl)
            .get()
            .header("User-Agent", "HomeAssistantStuga/1.0")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                AddressProbeResult.Reachable(
                    httpCode = response.code,
                    reasonPhrase = response.message
                )
            }
        } catch (e: IOException) {
            AddressProbeResult.Unreachable(e.message ?: e.javaClass.simpleName)
        }
    }

    suspend fun probeApi(baseUrl: String, token: String): ApiProbeResult = withContext(Dispatchers.IO) {
        val trimmedUrl = baseUrl.trim()
        if (trimmedUrl.isEmpty()) {
            return@withContext ApiProbeResult.InvalidInput("Enter a base URL")
        }
        if (token.isBlank()) {
            return@withContext ApiProbeResult.Skipped("Add a token to test the API")
        }
        // Match DynamicUrlInterceptor + Retrofit @GET("api/states"). A bare GET .../api often returns 404;
        // HA serves the REST API under /api/states, /api/services/..., etc.
        val apiUrl = mergeBaseWithPathSegments(trimmedUrl, listOf("api", "states"))
            ?: return@withContext ApiProbeResult.InvalidInput("Invalid URL")
        val request = Request.Builder()
            .url(apiUrl)
            .get()
            .header("Authorization", "Bearer ${token.trim()}")
            .header("Content-Type", "application/json")
            .header("User-Agent", "HomeAssistantStuga/1.0")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    ApiProbeResult.Ok
                } else {
                    val snippet = try {
                        response.body.string().trim().take(BODY_SNIPPET_MAX).takeIf { it.isNotEmpty() }
                    } catch (_: Exception) {
                        null
                    }
                    ApiProbeResult.HttpError(
                        code = response.code,
                        reasonPhrase = response.message,
                        bodySnippet = snippet
                    )
                }
            }
        } catch (e: IOException) {
            ApiProbeResult.NetworkError(e.message ?: e.javaClass.simpleName)
        }
    }

    companion object {
        private const val BODY_SNIPPET_MAX = 240

        /**
         * Same path merge as [DynamicUrlInterceptor]: user base path + Retrofit path segments.
         */
        private fun mergeBaseWithPathSegments(baseUrlString: String, pathSegments: List<String>): HttpUrl? {
            val newBase = baseUrlString.trim().toHttpUrlOrNull() ?: return null
            val builder = HttpUrl.Builder()
                .scheme(newBase.scheme)
                .host(newBase.host)
                .port(newBase.port)
            builder.encodedPath("/")
            for (segment in newBase.pathSegments) {
                if (segment.isNotEmpty()) {
                    builder.addPathSegment(segment)
                }
            }
            for (segment in pathSegments) {
                if (segment.isNotEmpty()) {
                    builder.addPathSegment(segment)
                }
            }
            return builder.build()
        }

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
}
