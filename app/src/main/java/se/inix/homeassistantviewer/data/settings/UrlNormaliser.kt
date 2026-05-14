package se.inix.homeassistantviewer.data.settings

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Validates and normalises user-entered Home Assistant base URLs.
 *
 * Kept as a pure object so both the data layer (when persisting a connection)
 * and the connections UI (live validation) call the same code path. Anything
 * accepted here must also be accepted by [DynamicUrlInterceptor].
 */
object UrlNormaliser {

    /**
     * - trims whitespace and trailing slashes
     * - adds `http://` if no scheme is present (typical when entering a LAN IP)
     * - rejects values that still fail to parse as an http(s) URL or that have
     *   no host portion
     *
     * Returns null when the input cannot be turned into a valid URL.
     */
    fun normalise(input: String): String? {
        val trimmed = input.trim().trimEnd('/')
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.contains("://")) trimmed
                         else "http://$trimmed"
        val parsed = withScheme.toHttpUrlOrNull() ?: return null
        if (parsed.host.isBlank()) return null
        return withScheme
    }

    fun isValid(input: String): Boolean = normalise(input) != null
}
