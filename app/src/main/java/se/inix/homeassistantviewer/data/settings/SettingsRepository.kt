@file:Suppress("DEPRECATION")

package se.inix.homeassistantviewer.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import se.inix.homeassistantviewer.data.model.FavoriteItem
import se.inix.homeassistantviewer.data.model.HaConnection

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * Façade over the four focused stores. Each concern (connections, favourites,
 * dashboard layout, theme) lives in its own file; this class exists so the rest
 * of the app keeps a single injection point and so the wiring between the
 * DataStore replay flow and the in-memory stores is in one place.
 */
class SettingsRepository(context: Context) {

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

    private val dataStore: DataStore<Preferences> = context.appDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val connectionsStore = ConnectionsStore(securePrefs)
    private val favoritesStore = FavoritesStore(dataStore, scope)
    private val dashboardPrefsStore = DashboardPreferencesStore(dataStore, scope)
    private val migration = LegacyPreferencesMigration(securePrefs, dataStore)

    val connections: StateFlow<List<HaConnection>> = connectionsStore.connections
    val favorites: StateFlow<List<FavoriteItem>> = favoritesStore.favorites
    val dashboardColumns: StateFlow<Int> = dashboardPrefsStore.columns
    val themeMode: StateFlow<ThemeMode> = dashboardPrefsStore.themeMode
    val colorPalette: StateFlow<ColorPalette> = dashboardPrefsStore.colorPalette

    init {
        scope.launch {
            migration.runIfNeeded()
            dataStore.data.collect { prefs ->
                favoritesStore.onDataStorePayload(prefs[FavoritesStore.KEY])
                dashboardPrefsStore.onDataStorePayload(prefs)
            }
        }
    }

    fun addConnection(name: String, baseUrl: String, token: String): HaConnection? =
        connectionsStore.add(name, baseUrl, token)

    fun updateConnection(id: String, name: String, baseUrl: String, token: String): Boolean =
        connectionsStore.update(id, name, baseUrl, token)

    fun deleteConnection(id: String) {
        connectionsStore.delete(id)
        favoritesStore.stripConnection(id)
    }

    fun toggleFavorite(connectionId: String, entityId: String) =
        favoritesStore.toggleEntity(connectionId, entityId)

    fun removeFavorite(item: FavoriteItem) = favoritesStore.remove(item)

    fun addDivider(): FavoriteItem.Divider = favoritesStore.addDivider()

    fun saveFavoriteOrder(ordered: List<FavoriteItem>) = favoritesStore.saveOrder(ordered)

    /** Renames a favourited entity. Pass `null` (or blank) to fall back to HA's `friendly_name`. */
    fun setFavoriteCustomName(connectionId: String, entityId: String, name: String?) =
        favoritesStore.setEntityCustomName(connectionId, entityId, name)

    /** Sets a divider's section heading. Pass `null` (or blank) to remove the heading. */
    fun setDividerTitle(id: String, title: String?) =
        favoritesStore.setDividerTitle(id, title)

    fun saveDashboardColumns(columns: Int) = dashboardPrefsStore.saveColumns(columns)

    fun saveThemeMode(mode: ThemeMode) = dashboardPrefsStore.saveThemeMode(mode)

    fun saveColorPalette(palette: ColorPalette) = dashboardPrefsStore.saveColorPalette(palette)

    companion object {
        /** Re-exposed so callers don't need a separate import for URL normalisation. */
        fun normaliseBaseUrl(input: String): String? = UrlNormaliser.normalise(input)
    }
}
