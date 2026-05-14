package se.inix.homeassistantviewer.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import se.inix.homeassistantviewer.data.model.FavoriteItem
import java.util.UUID

/**
 * Persists the user's dashboard list — both entity favourites and manually
 * inserted layout dividers — in DataStore (non-sensitive, plain).
 */
internal class FavoritesStore(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope
) {

    private val _favorites = MutableStateFlow<List<FavoriteItem>>(emptyList())
    val favorites: StateFlow<List<FavoriteItem>> = _favorites.asStateFlow()

    /** Called by [SettingsRepository] when DataStore replays/updates. */
    internal fun onDataStorePayload(raw: String?) {
        _favorites.value = deserialize(raw ?: "")
    }

    fun toggleEntity(connectionId: String, entityId: String) {
        val target = FavoriteItem.Entity(connectionId, entityId)
        val current = _favorites.value
        update(if (target in current) current - target else current + target)
    }

    fun remove(item: FavoriteItem) {
        update(_favorites.value.filterNot { it == item })
    }

    fun addDivider(): FavoriteItem.Divider {
        val divider = FavoriteItem.Divider(UUID.randomUUID().toString())
        update(_favorites.value + divider)
        return divider
    }

    fun saveOrder(ordered: List<FavoriteItem>) {
        update(ordered)
    }

    /** Removes all entity favourites belonging to [connectionId]; dividers are kept. */
    fun stripConnection(connectionId: String) {
        update(_favorites.value.filter { item ->
            when (item) {
                is FavoriteItem.Entity -> item.connectionId != connectionId
                is FavoriteItem.Divider -> true
            }
        })
    }

    private fun update(list: List<FavoriteItem>) {
        _favorites.value = list
        scope.launch { dataStore.edit { it[KEY] = serialize(list) } }
    }

    /**
     * Format: comma-separated tokens, each one of
     *  - `<connId>|<entityId>` (one `|`)  → entity favourite
     *  - `<uuid>`              (zero `|`) → divider
     * Backward-compatible with the previous "connId|entityId" only format.
     */
    private fun serialize(list: List<FavoriteItem>): String =
        list.joinToString(",") { item ->
            when (item) {
                is FavoriteItem.Entity -> "${item.connectionId}|${item.entityId}"
                is FavoriteItem.Divider -> item.id
            }
        }

    private fun deserialize(raw: String): List<FavoriteItem> =
        raw.split(",").filter { it.isNotBlank() }.mapNotNull { entry ->
            val pipe = entry.indexOf('|')
            when {
                pipe > 0 -> FavoriteItem.Entity(
                    connectionId = entry.substring(0, pipe),
                    entityId = entry.substring(pipe + 1)
                )
                pipe < 0 -> FavoriteItem.Divider(id = entry)
                else -> null
            }
        }

    companion object {
        internal val KEY = stringPreferencesKey("favorites")
    }
}
