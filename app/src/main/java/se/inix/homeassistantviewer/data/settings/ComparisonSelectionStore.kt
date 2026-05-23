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
import se.inix.homeassistantviewer.data.model.ComparisonEntity

/**
 * Persists entities the user has marked for the comparison graph view.
 * Independent of favourites order — selection survives navigation and restarts.
 */
internal class ComparisonSelectionStore(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope
) {

    private val _selection = MutableStateFlow<Set<ComparisonEntity>>(emptySet())
    val selection: StateFlow<Set<ComparisonEntity>> = _selection.asStateFlow()

    internal fun onDataStorePayload(raw: String?) {
        _selection.value = ComparisonSelectionCodec.deserialize(raw ?: "")
    }

    fun toggle(connectionId: String, entityId: String) {
        val entity = ComparisonEntity(connectionId, entityId)
        val current = _selection.value
        update(if (entity in current) current - entity else current + entity)
    }

    fun removeEntity(connectionId: String, entityId: String) {
        val entity = ComparisonEntity(connectionId, entityId)
        update(_selection.value - entity)
    }

    fun clear() {
        update(emptySet())
    }

    fun stripConnection(connectionId: String) {
        update(_selection.value.filter { it.connectionId != connectionId }.toSet())
    }

    fun replaceAll(selection: Set<ComparisonEntity>) {
        update(selection)
    }

    private fun update(selection: Set<ComparisonEntity>) {
        _selection.value = selection
        scope.launch {
            dataStore.edit { it[KEY] = ComparisonSelectionCodec.serialize(selection) }
        }
    }

    companion object {
        internal val KEY = stringPreferencesKey("comparison_selection")
    }
}
