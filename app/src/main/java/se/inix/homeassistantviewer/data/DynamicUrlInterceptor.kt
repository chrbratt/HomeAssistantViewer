package se.inix.homeassistantviewer.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

class DynamicUrlInterceptor(
    private val baseUrl: String,
    private val token: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if (baseUrl.isNotEmpty()) {
            val newBaseUrl = baseUrl.toHttpUrlOrNull()
            if (newBaseUrl != null) {
                val originalPathSegments = request.url.pathSegments
                val newUrlBuilder = request.url.newBuilder()
                    .scheme(newBaseUrl.scheme)
                    .host(newBaseUrl.host)
                    .port(newBaseUrl.port)

                newUrlBuilder.encodedPath("/")
                for (pathSegment in newBaseUrl.pathSegments) {
                    if (pathSegment.isNotEmpty()) newUrlBuilder.addPathSegment(pathSegment)
                }
                for (pathSegment in originalPathSegments) {
                    newUrlBuilder.addPathSegment(pathSegment)
                }

                request = request.newBuilder().url(newUrlBuilder.build()).build()
            }
        }

        request = request.newBuilder()
            .addHeader("Content-Type", "application/json")
            .build()

        if (token.isNotEmpty()) {
            request = request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }

        return chain.proceed(request)
    }
}
