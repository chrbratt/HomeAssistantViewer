package se.inix.homeassistantviewer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import se.inix.homeassistantviewer.data.ConnectionPool
import se.inix.homeassistantviewer.data.SettingsRepository
import se.inix.homeassistantviewer.data.model.HaConnection
import se.inix.homeassistantviewer.data.model.HaEntityState

sealed class EntityPickerUiState {
    data object Loading : EntityPickerUiState()
    data class Success(
        val groupedEntities: List<Pair<String, List<HaEntityState>>>,
        val favoriteEntityIds: Set<String>,
        val availableCategories: List<String>
    ) : EntityPickerUiState()
    data class Error(val message: String) : EntityPickerUiState()
}

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

    val searchQuery = MutableStateFlow("")
    val categoryFilter = MutableStateFlow<String?>(null) // null = All

    val uiState: StateFlow<EntityPickerUiState> = combine(
        _allEntities,
        searchQuery,
        categoryFilter,
        _isLoading,
        _error,
        settingsRepository.favoriteEntities,
        _selectedConnectionId
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val entities = args[0] as List<HaEntityState>
        val query = args[1] as String
        val category = args[2] as String?
        val loading = args[3] as Boolean
        val error = args[4] as String?
        val favorites = args[5] as List<*>
        val connId = args[6] as String

        when {
            loading -> EntityPickerUiState.Loading
            error != null -> EntityPickerUiState.Error(error)
            else -> {
                val filtered = entities.filter { entity ->
                    matchesSmartSearch(entity, query) &&
                        (category == null || entity.domain == category)
                }
                val grouped = filtered
                    .groupBy { it.domain }
                    .entries
                    .sortedWith(Comparator { a, b ->
                        val ai = DOMAIN_PRIORITY.indexOf(a.key).let { if (it < 0) Int.MAX_VALUE else it }
                        val bi = DOMAIN_PRIORITY.indexOf(b.key).let { if (it < 0) Int.MAX_VALUE else it }
                        if (ai != bi) ai.compareTo(bi) else a.key.compareTo(b.key)
                    })
                    .map { it.toPair() }

                val favIds = favorites
                    .filterIsInstance<se.inix.homeassistantviewer.data.model.FavoriteEntity>()
                    .filter { it.connectionId == connId }
                    .map { it.entityId }
                    .toSet()

                val available = entities.map { it.domain }.distinct()
                    .sortedWith(Comparator { a, b ->
                        val ai = DOMAIN_PRIORITY.indexOf(a).let { if (it < 0) Int.MAX_VALUE else it }
                        val bi = DOMAIN_PRIORITY.indexOf(b).let { if (it < 0) Int.MAX_VALUE else it }
                        if (ai != bi) ai.compareTo(bi) else a.compareTo(b)
                    })

                EntityPickerUiState.Success(grouped, favIds, available)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntityPickerUiState.Loading)

    init {
        viewModelScope.launch {
            settingsRepository.connections.collect { conns ->
                if (_selectedConnectionId.value.isBlank() && conns.isNotEmpty()) {
                    selectConnection(conns.first().id)
                }
            }
        }
    }

    fun selectConnection(connectionId: String) {
        if (_selectedConnectionId.value == connectionId) return
        _selectedConnectionId.value = connectionId
        categoryFilter.value = null
        searchQuery.value = ""
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
                _error.value = e.message ?: "Failed to load entities"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite(entityId: String) {
        val connId = _selectedConnectionId.value.ifBlank { return }
        settingsRepository.toggleFavorite(connId, entityId)
    }

    companion object {
        /**
         * Multi-word smart search: ALL space-separated tokens must appear somewhere in
         * the combined searchable text (friendly name + entity ID).
         *
         * Example: "temperature utomhus" finds an entity whose friendly name is
         * "Utomhus Temperatur" and whose ID contains "utomhus" and "temperature".
         */
        fun matchesSmartSearch(entity: HaEntityState, query: String): Boolean {
            if (query.isBlank()) return true
            val text = listOfNotNull(entity.friendlyName, entity.entityId)
                .joinToString(" ")
            return query.trim().split(Regex("\\s+"))
                .all { token -> text.contains(token, ignoreCase = true) }
        }

        private val DOMAIN_PRIORITY = listOf(
            "light", "switch", "climate", "cover", "fan", "lock",
            "media_player", "sensor", "binary_sensor",
            "input_boolean", "automation", "scene", "script"
        )
    }
}
