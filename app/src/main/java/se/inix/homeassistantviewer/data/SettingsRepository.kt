// EncryptedSharedPreferences is deprecated since security-crypto 1.1.0-alpha06.
// It is intentionally kept here exclusively for connection credentials (baseUrl + token)
// since those are genuinely sensitive. Non-sensitive settings (favorites, columns) have
// been migrated to DataStore. Remove this suppression when Jetpack Security ships a
// stable encrypted DataStore API.
@file:Suppress("DEPRECATION")

package se.inix.homeassistantviewer.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import se.inix.homeassistantviewer.data.SettingsRepository.Companion.DS_MIGRATED
import se.inix.homeassistantviewer.data.model.FavoriteEntity
import se.inix.homeassistantviewer.data.model.HaConnection
import java.util.UUID

/** Controls which colour scheme the app uses, independently of the system setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

// Singleton DataStore instance — one per application context.
private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class SettingsRepository(context: Context) {

    // ── Sensitive storage — EncryptedSharedPreferences ─────────────────────────
    // Used ONLY for connection credentials (id, name, baseUrl, token).
    // All non-sensitive settings live in DataStore below.
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_connections",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ── Non-sensitive storage — DataStore ───────────────────────────────────────
    private val dataStore: DataStore<Preferences> = context.appDataStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── State flows ─────────────────────────────────────────────────────────────

    private val _connections = MutableStateFlow<List<HaConnection>>(emptyList())
    val connections: StateFlow<List<HaConnection>> = _connections.asStateFlow()

    /**
     * Ordered list of favorited entities. The list order defines the display order
     * on the dashboard; new favorites are appended at the end.
     * Backed by DataStore — populated asynchronously after construction.
     */
    private val _favoriteEntities = MutableStateFlow<List<FavoriteEntity>>(emptyList())
    val favoriteEntities: StateFlow<List<FavoriteEntity>> = _favoriteEntities.asStateFlow()

    /**
     * Number of columns on the dashboard grid (1–3).
     * Backed by DataStore — populated asynchronously after construction.
     */
    private val _dashboardColumns = MutableStateFlow(DEFAULT_COLUMNS)
    val dashboardColumns: StateFlow<Int> = _dashboardColumns.asStateFlow()

    /**
     * User-selected theme override.
     * [ThemeMode.SYSTEM] (default) follows the Android system setting.
     * Backed by DataStore — populated asynchronously after construction.
     */
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        // Load connections synchronously — they live in EncryptedSharedPreferences
        // and are needed immediately when the pool starts observing this flow.
        _connections.value = loadConnections()

        // Collect DataStore for non-sensitive settings.
        // Also performs a one-time migration from the old EncryptedSharedPreferences
        // entries so existing users keep their favorites and column preference.
        scope.launch {
            migrateNonSensitiveDataIfNeeded()
            dataStore.data.collect { prefs ->
                _favoriteEntities.value = deserializeFavorites(prefs[DS_FAVORITES] ?: "")
                _dashboardColumns.value = prefs[DS_COLUMNS] ?: DEFAULT_COLUMNS
                _themeMode.value = prefs[DS_THEME]
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM
            }
        }
    }

    /**
     * Copies favorites and column count from the old all-in-one EncryptedSharedPreferences
     * file to DataStore. Runs exactly once, guarded by [DS_MIGRATED].
     */
    private suspend fun migrateNonSensitiveDataIfNeeded() {
        val alreadyMigrated = dataStore.data.first()[DS_MIGRATED] == true
        if (alreadyMigrated) return

        val oldFavs = securePrefs.getString(OLD_KEY_FAVORITES, null)
        val oldCols = securePrefs.getInt(OLD_KEY_COLUMNS, DEFAULT_COLUMNS)

        // Also handle the very-old format where favorites were plain entity IDs.
        val legacyFavs = securePrefs.getString(LEGACY_KEY_FAVORITES, null)
        val resolvedFavs = when {
            !oldFavs.isNullOrBlank() -> oldFavs
            !legacyFavs.isNullOrBlank() -> legacyFavs
                .split(",").filter { it.isNotBlank() }
                .joinToString(",") { "$LEGACY_CONNECTION_ID|$it" }
            else -> ""
        }

        dataStore.edit { prefs ->
            if (resolvedFavs.isNotBlank()) prefs[DS_FAVORITES] = resolvedFavs
            prefs[DS_COLUMNS] = oldCols
            prefs[DS_MIGRATED] = true
        }
    }

    // ── Connections ─────────────────────────────────────────────────────────────

    fun addConnection(name: String, baseUrl: String, token: String): HaConnection {
        val conn = HaConnection(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Home Assistant" },
            baseUrl = baseUrl.trim(),
            token = token.trim()
        )
        persistConnections(_connections.value + conn)
        return conn
    }

    fun updateConnection(id: String, name: String, baseUrl: String, token: String) {
        persistConnections(_connections.value.map { conn ->
            if (conn.id == id) conn.copy(
                name = name.ifBlank { conn.name },
                baseUrl = baseUrl.trim(),
                token = token.trim()
            ) else conn
        })
    }

    fun deleteConnection(id: String) {
        persistConnections(_connections.value.filter { it.id != id })
        // Also remove favorites that belonged to this connection.
        updateFavorites(_favoriteEntities.value.filter { it.connectionId != id })
    }

    // ── Favorites ───────────────────────────────────────────────────────────────

    /** Appends the entity to the list, or removes it if already present. */
    fun toggleFavorite(connectionId: String, entityId: String) {
        val key = FavoriteEntity(connectionId, entityId)
        val current = _favoriteEntities.value
        updateFavorites(if (key in current) current - key else current + key)
    }

    /** Persists a new display order after the user drags a card to a new position. */
    fun saveFavoriteOrder(ordered: List<FavoriteEntity>) {
        updateFavorites(ordered)
    }

    // ── Dashboard layout ─────────────────────────────────────────────────────────

    fun saveDashboardColumns(columns: Int) {
        require(columns in 1..3) { "Column count must be 1, 2, or 3" }
        _dashboardColumns.value = columns  // Optimistic update for instant UI response.
        scope.launch { dataStore.edit { it[DS_COLUMNS] = columns } }
    }

    fun saveThemeMode(mode: ThemeMode) {
        _themeMode.value = mode  // Optimistic update — theme switches immediately.
        scope.launch { dataStore.edit { it[DS_THEME] = mode.name } }
    }

    // ── Internal persistence helpers ─────────────────────────────────────────────

    private fun persistConnections(list: List<HaConnection>) {
        securePrefs.edit().putString(KEY_CONNECTIONS, serializeConnections(list)).apply()
        _connections.value = list
    }

    private fun updateFavorites(list: List<FavoriteEntity>) {
        _favoriteEntities.value = list  // Optimistic update for instant UI response.
        scope.launch { dataStore.edit { it[DS_FAVORITES] = serializeFavorites(list) } }
    }

    private fun loadConnections(): List<HaConnection> {
        val json = securePrefs.getString(KEY_CONNECTIONS, null)
        if (!json.isNullOrBlank()) return deserializeConnections(json)

        // Migrate legacy single-connection credentials stored under the old key.
        val legacyUrl = securePrefs.getString(LEGACY_KEY_URL, "")
        val legacyToken = securePrefs.getString(LEGACY_KEY_TOKEN, "")
        if (!legacyUrl.isNullOrBlank() && !legacyToken.isNullOrBlank()) {
            val conn = HaConnection(
                id = LEGACY_CONNECTION_ID,
                name = "Home Assistant",
                baseUrl = legacyUrl,
                token = legacyToken
            )
            securePrefs.edit().putString(KEY_CONNECTIONS, serializeConnections(listOf(conn))).apply()
            return listOf(conn)
        }
        return emptyList()
    }

    // ── Serialization ─────────────────────────────────────────────────────────────

    private fun serializeConnections(list: List<HaConnection>): String =
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

    private fun deserializeConnections(json: String): List<HaConnection> = try {
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

    /** Format: "connId|entityId,connId|entityId,…" — order = display order. */
    private fun serializeFavorites(list: List<FavoriteEntity>): String =
        list.joinToString(",") { "${it.connectionId}|${it.entityId}" }

    private fun deserializeFavorites(raw: String): List<FavoriteEntity> =
        raw.split(",").filter { it.isNotBlank() }.mapNotNull { entry ->
            val idx = entry.indexOf('|')
            if (idx < 0) null
            else FavoriteEntity(entry.substring(0, idx), entry.substring(idx + 1))
        }

    companion object {
        // ── EncryptedSharedPreferences keys (credentials only) ─────────────────
        private const val KEY_CONNECTIONS = "connections_json"

        // ── DataStore keys (non-sensitive settings) ───────────────────────────
        private val DS_FAVORITES = stringPreferencesKey("favorites")
        private val DS_COLUMNS   = intPreferencesKey("columns")
        private val DS_THEME     = stringPreferencesKey("theme_mode")
        private val DS_MIGRATED  = booleanPreferencesKey("migrated_from_esp")

        // ── Legacy keys — kept only for one-time migration ─────────────────────
        private const val LEGACY_KEY_URL       = "base_url"
        private const val LEGACY_KEY_TOKEN     = "token"
        private const val LEGACY_KEY_FAVORITES = "favorite_entity_ids"
        private const val OLD_KEY_FAVORITES    = "favorite_entities_v2"
        private const val OLD_KEY_COLUMNS      = "dashboard_columns"

        const val LEGACY_CONNECTION_ID = "default"
        const val DEFAULT_COLUMNS = 2
    }
}
