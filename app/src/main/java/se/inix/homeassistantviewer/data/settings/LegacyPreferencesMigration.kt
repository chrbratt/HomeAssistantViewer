@file:Suppress("DEPRECATION")

package se.inix.homeassistantviewer.data.settings

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

/**
 * One-time migration from the original all-in-one EncryptedSharedPreferences
 * (favorites + columns + theme) to DataStore. Idempotent — guarded by a flag
 * stored in DataStore itself.
 */
internal class LegacyPreferencesMigration(
    private val securePrefs: SharedPreferences,
    private val dataStore: DataStore<Preferences>
) {

    suspend fun runIfNeeded() {
        val alreadyMigrated = dataStore.data.first()[KEY_MIGRATED] == true
        if (alreadyMigrated) return

        val oldFavs = securePrefs.getString(OLD_KEY_FAVORITES, null)
        val oldCols = securePrefs.getInt(OLD_KEY_COLUMNS, DashboardPreferencesStore.DEFAULT_COLUMNS)

        val legacyFavs = securePrefs.getString(LEGACY_KEY_FAVORITES, null)
        val resolvedFavs = when {
            !oldFavs.isNullOrBlank() -> oldFavs
            !legacyFavs.isNullOrBlank() -> legacyFavs
                .split(",").filter { it.isNotBlank() }
                .joinToString(",") { "${ConnectionsStore.LEGACY_CONNECTION_ID}|$it" }
            else -> ""
        }

        dataStore.edit { prefs ->
            if (resolvedFavs.isNotBlank()) prefs[FavoritesStore.KEY] = resolvedFavs
            prefs[DashboardPreferencesStore.KEY_COLUMNS] = oldCols
            prefs[KEY_MIGRATED] = true
        }
    }

    companion object {
        private val KEY_MIGRATED = booleanPreferencesKey("migrated_from_esp")
        private const val LEGACY_KEY_FAVORITES = "favorite_entity_ids"
        private const val OLD_KEY_FAVORITES    = "favorite_entities_v2"
        private const val OLD_KEY_COLUMNS      = "dashboard_columns"
    }
}
