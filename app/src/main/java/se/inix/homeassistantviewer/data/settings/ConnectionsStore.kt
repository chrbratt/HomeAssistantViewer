// EncryptedSharedPreferences is deprecated since security-crypto 1.1.0-alpha06.
// It is intentionally kept here for connection credentials (baseUrl + token) which
// are genuinely sensitive. Remove this suppression once Jetpack Security ships a
// stable encrypted DataStore API.
@file:Suppress("DEPRECATION")

package se.inix.homeassistantviewer.data.settings

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import se.inix.homeassistantviewer.data.model.HaConnection
import java.util.UUID

/**
 * Persists Home Assistant connections (URL + token) in EncryptedSharedPreferences.
 * Exposes [connections] as a StateFlow so the rest of the app can observe changes
 * without touching SharedPreferences directly.
 */
internal class ConnectionsStore(
    private val securePrefs: SharedPreferences
) {

    private val _connections = MutableStateFlow<List<HaConnection>>(emptyList())
    val connections: StateFlow<List<HaConnection>> = _connections.asStateFlow()

    init {
        _connections.value = loadConnections()
    }

    fun add(name: String, baseUrl: String, token: String): HaConnection? {
        val normalised = UrlNormaliser.normalise(baseUrl) ?: return null
        val conn = HaConnection(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Home Assistant" },
            baseUrl = normalised,
            token = token.trim()
        )
        persist(_connections.value + conn)
        return conn
    }

    fun update(id: String, name: String, baseUrl: String, token: String): Boolean {
        val normalised = UrlNormaliser.normalise(baseUrl) ?: return false
        persist(_connections.value.map { conn ->
            if (conn.id == id) conn.copy(
                name = name.ifBlank { conn.name },
                baseUrl = normalised,
                token = token.trim()
            ) else conn
        })
        return true
    }

    fun delete(id: String) {
        persist(_connections.value.filter { it.id != id })
    }

    private fun persist(list: List<HaConnection>) {
        securePrefs.edit().putString(KEY_CONNECTIONS, serialize(list)).apply()
        _connections.value = list
    }

    private fun loadConnections(): List<HaConnection> {
        val json = securePrefs.getString(KEY_CONNECTIONS, null)
        if (!json.isNullOrBlank()) return deserialize(json)

        // One-time pickup of pre-multi-connection credentials.
        val legacyUrl = securePrefs.getString(LEGACY_KEY_URL, "")
        val legacyToken = securePrefs.getString(LEGACY_KEY_TOKEN, "")
        if (!legacyUrl.isNullOrBlank() && !legacyToken.isNullOrBlank()) {
            val conn = HaConnection(
                id = LEGACY_CONNECTION_ID,
                name = "Home Assistant",
                baseUrl = legacyUrl,
                token = legacyToken
            )
            securePrefs.edit().putString(KEY_CONNECTIONS, serialize(listOf(conn))).apply()
            return listOf(conn)
        }
        return emptyList()
    }

    private fun serialize(list: List<HaConnection>): String =
        JSONArray().apply {
            list.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("baseUrl", c.baseUrl)
                    put("token", c.token)
                })
            }
        }.toString()

    private fun deserialize(json: String): List<HaConnection> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            HaConnection(
                id = o.getString("id"),
                name = o.getString("name"),
                baseUrl = o.getString("baseUrl"),
                token = o.getString("token")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    companion object {
        const val LEGACY_CONNECTION_ID = "default"
        private const val KEY_CONNECTIONS = "connections_json"
        private const val LEGACY_KEY_URL = "base_url"
        private const val LEGACY_KEY_TOKEN = "token"
    }
}
