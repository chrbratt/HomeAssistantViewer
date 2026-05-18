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
 *
 * Format note: when the user introduced optional custom names (for entities)
 * and section titles (for dividers) we kept the on-disk format
 * backwards-compatible — old payloads still parse without migration. The
 * writer always emits the new prefix-encoded form (`e:` / `d:`), so the next
 * mutation effectively migrates the user transparently. See [serialize] /
 * [deserialize] for the exact grammar.
 */
internal class FavoritesStore(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope
) {

    private val _favorites = MutableStateFlow<List<FavoriteItem>>(emptyList())
    val favorites: StateFlow<List<FavoriteItem>> = _favorites.asStateFlow()

    /** Called by [SettingsRepository] when DataStore replays/updates. */
    internal fun onDataStorePayload(raw: String?) {
        _favorites.value = FavoritesCodec.deserialize(raw ?: "")
    }

    fun toggleEntity(connectionId: String, entityId: String) {
        // Equality respects only the (connectionId, entityId) discriminator
        // by way of the [FavoriteItem.Entity] data class — but `customName`
        // is part of equality, so we match by *key* to avoid the
        // "togging-on a renamed favourite leaves a duplicate" trap.
        val current = _favorites.value
        val targetKey = FavoriteItem.Entity(connectionId, entityId).key
        val existing = current.firstOrNull { it.key == targetKey }
        update(if (existing != null) current - existing else current + FavoriteItem.Entity(connectionId, entityId))
    }

    fun remove(item: FavoriteItem) {
        // Match by key so an outdated copy (e.g. without the freshly-set
        // customName) still removes the right favourite.
        update(_favorites.value.filterNot { it.key == item.key })
    }

    fun addDivider(): FavoriteItem.Divider {
        val divider = FavoriteItem.Divider(UUID.randomUUID().toString())
        update(_favorites.value + divider)
        return divider
    }

    fun saveOrder(ordered: List<FavoriteItem>) {
        update(ordered)
    }

    /**
     * Sets (or clears, via `null` / blank) the custom name on the matching
     * entity favourite. No-op if the entity is not currently favourited —
     * we never *create* a favourite as a side-effect of renaming.
     */
    fun setEntityCustomName(connectionId: String, entityId: String, name: String?) {
        val normalised = name?.trim()?.takeUnless { it.isEmpty() }
        update(
            _favorites.value.map { item ->
                if (item is FavoriteItem.Entity &&
                    item.connectionId == connectionId &&
                    item.entityId == entityId
                ) item.copy(customName = normalised)
                else item
            }
        )
    }

    /** Sets (or clears) the title on the matching divider. No-op if the id is unknown. */
    fun setDividerTitle(id: String, title: String?) {
        val normalised = title?.trim()?.takeUnless { it.isEmpty() }
        update(
            _favorites.value.map { item ->
                if (item is FavoriteItem.Divider && item.id == id) item.copy(title = normalised)
                else item
            }
        )
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

    /** Replaces the full favourites list — used by backup restore. */
    fun replaceAll(list: List<FavoriteItem>) {
        update(list)
    }

    private fun update(list: List<FavoriteItem>) {
        _favorites.value = list
        scope.launch { dataStore.edit { it[KEY] = FavoritesCodec.serialize(list) } }
    }

    companion object {
        internal val KEY = stringPreferencesKey("favorites")
    }
}
