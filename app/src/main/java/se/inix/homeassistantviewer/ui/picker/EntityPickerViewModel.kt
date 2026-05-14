package se.inix.homeassistantviewer.ui.picker

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import se.inix.homeassistantviewer.data.model.FavoriteItem
import se.inix.homeassistantviewer.data.model.HaConnection
import se.inix.homeassistantviewer.data.model.HaEntityState
import se.inix.homeassistantviewer.data.settings.SettingsRepository
import se.inix.homeassistantviewer.data.ws.ConnectionPool
import se.inix.homeassistantviewer.ui.picker.EntityPickerViewModel.Companion.DOMAIN_PRIORITY

sealed class EntityPickerUiState {
    data object Loading : EntityPickerUiState()
    data class Success(
        val groupedEntities: List<Pair<String, List<HaEntityState>>>,
        val favoriteEntityIds: Set<String>,
        val availableCategories: List<String>
    ) : EntityPickerUiState()
    data class Error(val message: String) : EntityPickerUiState()
}

@OptIn(FlowPreview::class)
class EntityPickerViewModel(
    private val connectionPool: ConnectionPool,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val connections: StateFlow<List<HaConnection>> = settingsRepository.connections

    private val _selectedConnectionId = MutableStateFlow("")
    val selectedConnectionId: StateFlow<String> = _selectedConnectionId

    private val _allEntities = MutableStateFlow<List<HaEntityState>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val searchQuery = TextFieldState()
    val categoryFilter = MutableStateFlow<String?>(null) // null = All
    val showFavoritesOnly = MutableStateFlow(false)

    val uiState: StateFlow<EntityPickerUiState> = combine(
        _allEntities,
        // Debouncing keeps the heavy filter+sort pipeline off the recomposition
        // path on devices with hundreds of HA entities.
        snapshotFlow { searchQuery.text.toString() }.debounce(80L),
        categoryFilter,
        _isLoading,
        _error,
        settingsRepository.favorites,
        _selectedConnectionId,
        showFavoritesOnly
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val entities = args[0] as List<HaEntityState>
        val query = args[1] as String
        val category = args[2] as String?
        val loading = args[3] as Boolean
        val error = args[4] as String?
        val favorites = args[5] as List<*>
        val connId = args[6] as String
        val favoritesOnly = args[7] as Boolean

        when {
            loading -> EntityPickerUiState.Loading
            error != null -> EntityPickerUiState.Error(error)
            else -> {
                val favIds = favorites
                    .filterIsInstance<FavoriteItem.Entity>()
                    .filter { it.connectionId == connId }
                    .map { it.entityId }
                    .toSet()

                val filtered = entities.filter { entity ->
                    matchesSmartSearch(entity, query) &&
                        (category == null || entity.domain == category) &&
                        (!favoritesOnly || entity.entityId in favIds)
                }
                val grouped = filtered
                    .groupBy { it.domain }
                    .entries
                    .sortedWith(domainPriorityComparator { it.key })
                    .map { it.toPair() }

                val available = entities.map { it.domain }.distinct()
                    .sortedWith(domainPriorityComparator { it })

                EntityPickerUiState.Success(grouped, favIds, available)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntityPickerUiState.Loading)

    init {
        viewModelScope.launch {
            settingsRepository.connections.collect { conns ->
                val current = _selectedConnectionId.value
                val needsSelection = current.isBlank() || conns.none { it.id == current }
                if (needsSelection && conns.isNotEmpty()) {
                    selectConnection(conns.first().id)
                } else if (conns.isEmpty()) {
                    _selectedConnectionId.value = ""
                    _allEntities.value = emptyList()
                    _isLoading.value = false
                    _error.value = null
                }
            }
        }
    }

    fun selectConnection(connectionId: String) {
        if (_selectedConnectionId.value == connectionId) return
        _selectedConnectionId.value = connectionId
        // Keep the search text so users can compare the same entity name across
        // servers without retyping.
        categoryFilter.value = null
        showFavoritesOnly.value = false
        loadEntities()
    }

    fun loadEntities() {
        val connId = _selectedConnectionId.value.ifBlank { return }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _allEntities.value = emptyList()
            try {
                _allEntities.value = connectionPool.repositoryFor(connId)?.getStates() ?: emptyList()
            } catch (e: Exception) {
                _error.value = mapLoadError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSearch() {
        searchQuery.edit { replace(0, length, "") }
    }

    fun toggleFavorite(entityId: String) {
        val connId = _selectedConnectionId.value.ifBlank { return }
        settingsRepository.toggleFavorite(connId, entityId)
    }

    /** Appends a divider to the favorites list. Visible only on the dashboard. */
    fun addDivider() {
        settingsRepository.addDivider()
    }

    companion object {
        /**
         * Multi-word smart search: ALL space-separated tokens must appear somewhere
         * in the combined searchable text (friendly name + entity ID).
         */
        fun matchesSmartSearch(entity: HaEntityState, query: String): Boolean {
            if (query.isBlank()) return true
            val text = listOfNotNull(entity.friendlyName, entity.entityId).joinToString(" ")
            return query.trim().split(Regex("\\s+"))
                .all { token -> text.contains(token, ignoreCase = true) }
        }

        private val DOMAIN_PRIORITY = listOf(
            "light", "switch", "climate", "cover", "fan", "lock",
            "media_player", "sensor", "binary_sensor",
            "input_boolean", "automation", "scene", "script"
        )

        /** Sort by [DOMAIN_PRIORITY] first, then alphabetically. */
        private fun <T> domainPriorityComparator(selector: (T) -> String): Comparator<T> =
            Comparator { a, b ->
                val sa = selector(a); val sb = selector(b)
                val ai = DOMAIN_PRIORITY.indexOf(sa).let { if (it < 0) Int.MAX_VALUE else it }
                val bi = DOMAIN_PRIORITY.indexOf(sb).let { if (it < 0) Int.MAX_VALUE else it }
                if (ai != bi) ai.compareTo(bi) else sa.compareTo(sb)
            }

        /**
         * Translates low-level errors into something the user can act on. Most
         * HTTP failures from Retrofit are unhelpful by default; auth failures in
         * particular deserve specific wording.
         */
        private fun mapLoadError(e: Exception): String {
            val msg = e.message.orEmpty()
            return when {
                msg.contains("401") || msg.contains("Unauthorized", ignoreCase = true) ->
                    "Authentication failed. Check the access token in Settings → Connections."
                msg.contains("403") || msg.contains("Forbidden", ignoreCase = true) ->
                    "Access denied. The token does not have permission to read states."
                msg.contains("404") ->
                    "Server reached but the Home Assistant API was not found at this URL."
                msg.contains("timeout", ignoreCase = true) ->
                    "Could not reach the server. Check the URL and the network."
                msg.isBlank() -> "Failed to load entities."
                else -> msg
            }
        }
    }
}
