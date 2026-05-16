package se.inix.homeassistantviewer.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import se.inix.homeassistantviewer.data.model.HaEntityState
import se.inix.homeassistantviewer.domain.history.HistoryPoint
import se.inix.homeassistantviewer.domain.history.HistoryRange
import se.inix.homeassistantviewer.domain.history.HistorySeries
import se.inix.homeassistantviewer.domain.history.HistorySeriesBuilder
import se.inix.homeassistantviewer.domain.history.SeriesClassifier
import java.time.Instant

/**
 * State holder for the entity detail screen.
 *
 * Responsibilities (intentionally narrow):
 *  - hold the user-selected [HistoryRange]
 *  - fetch the history series lazily when the range changes
 *  - keep the chart's current-value display in sync with live WS updates
 *
 * Lazy-loading contract:
 *  - no history request goes out until the screen is opened (this VM exists)
 *  - going back closes the VM scope, releasing the series from memory
 *  - per-range cache is intra-VM only, so revisiting the screen refetches
 *    fresh data (KISS — no cross-screen caching layer to invalidate)
 */
internal class EntityDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val dataSource: EntityHistoryDataSource,
    private val now: () -> Instant = Instant::now
) : ViewModel() {

    val entityId: String = requireNotNull(savedStateHandle[ARG_ENTITY_ID]) {
        "EntityDetailViewModel requires '$ARG_ENTITY_ID' nav argument"
    }

    private val _selectedRange = MutableStateFlow(HistoryRange.Default)
    val selectedRange: StateFlow<HistoryRange> = _selectedRange.asStateFlow()

    private val _uiState = MutableStateFlow<EntityDetailUiState>(EntityDetailUiState.Loading)
    val uiState: StateFlow<EntityDetailUiState> = _uiState.asStateFlow()

    /**
     * Per-range cache so flipping between 1h/24h/7d doesn't cause repeated
     * network round-trips within a single screen visit.
     */
    private val seriesCache = mutableMapOf<HistoryRange, HistorySeries>()

    @Volatile private var latestCurrentState: HaEntityState? = null

    private var fetchJob: Job? = null

    init {
        observeStateChanges()
        fetchForRange(_selectedRange.value)
    }

    /**
     * Called when the user taps a different time-range chip. No-op if the
     * range is already selected so chip taps don't cancel an in-flight fetch
     * for the same range.
     */
    fun selectRange(range: HistoryRange) {
        if (_selectedRange.value == range) return
        _selectedRange.value = range
        fetchForRange(range)
    }

    /** Forces a refetch of the currently selected range, bypassing the cache. */
    fun refresh() {
        seriesCache.remove(_selectedRange.value)
        fetchForRange(_selectedRange.value)
    }

    private fun fetchForRange(range: HistoryRange) {
        val cached = seriesCache[range]
        if (cached != null) {
            _uiState.value = if (!cached.hasPlottableData())
                EntityDetailUiState.Empty(latestCurrentState)
            else
                EntityDetailUiState.Loaded(cached, latestCurrentState)
            return
        }
        _uiState.value = EntityDetailUiState.Loading
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            val end = now()
            val start = end.minus(range.duration)
            val result = runCatching {
                // History + current state in parallel so the screen renders as
                // soon as both finish; either alone is already much slower than
                // the await of the other on typical home networks.
                coroutineScope {
                    val historyDeferred = async { dataSource.getHistory(entityId, start, end) }
                    val currentDeferred = async { dataSource.getCurrentState(entityId) }
                    val rows = historyDeferred.await()
                    val current = currentDeferred.await()
                    latestCurrentState = current ?: latestCurrentState
                    val domain = current?.entityId?.substringBefore(".") ?: entityId.substringBefore(".")
                    val unit = current?.unitOfMeasurement
                    HistorySeriesBuilder.build(rows, domain, unit)
                }
            }
            result.onSuccess { series ->
                seriesCache[range] = series
                // "Empty" means no plottable data — either zero rows or every
                // row was unparseable (e.g. all "unavailable"). The chart needs
                // at least 2 numeric points to render anything meaningful.
                _uiState.value = if (!series.hasPlottableData())
                    EntityDetailUiState.Empty(latestCurrentState)
                else
                    EntityDetailUiState.Loaded(series, latestCurrentState)
            }.onFailure { t ->
                _uiState.value = EntityDetailUiState.Error(
                    t.message?.takeIf { it.isNotBlank() } ?: "Failed to load history"
                )
            }
        }
    }

    /**
     * Mirrors live updates from the WebSocket into the screen state.
     *
     * Two things happen per event:
     *  1. The top-of-screen "current value" is updated.
     *  2. If we already have a [HistorySeries] for the selected range, the
     *     new state is appended to the series so the chart's right edge
     *     stays live (the user asked for a real-time graph).
     *
     * We do **not** rebuild / re-downsample the series — appending is O(1)
     * per event and the chart re-renders only the points that changed.
     * Re-fetching from HA would defeat the purpose and burn data.
     */
    private fun observeStateChanges() {
        viewModelScope.launch {
            dataSource.stateChanges.collect { newState ->
                if (newState.entityId != entityId) return@collect
                latestCurrentState = newState
                applyLiveUpdate(newState)
            }
        }
    }

    private fun applyLiveUpdate(newState: HaEntityState) {
        val current = _uiState.value
        val existingSeries = (current as? EntityDetailUiState.Loaded)?.series
        if (existingSeries == null) {
            // No series yet — just refresh the current value in the headline.
            _uiState.value = when (current) {
                is EntityDetailUiState.Empty -> current.copy(currentState = newState)
                else -> current
            }
            return
        }

        val ts = runCatching { Instant.parse(newState.lastChanged) }.getOrNull()
        val projected = SeriesClassifier.project(existingSeries.kind, newState.state)
        val lastTs = existingSeries.points.lastOrNull()?.timestamp

        // Only append if the new state is strictly newer than the rightmost
        // point. HA emits one event per actual state change, so duplicates
        // here would mean either a clock jitter or a redundant push — both
        // safe to ignore.
        val shouldAppend = ts != null && (lastTs == null || ts.isAfter(lastTs))
        val updatedSeries = if (shouldAppend) {
            val newPoint = HistoryPoint(
                timestamp = ts!!,
                value = projected,
                rawState = newState.state
            )
            existingSeries.copy(points = existingSeries.points + newPoint)
        } else {
            existingSeries
        }

        if (shouldAppend) {
            seriesCache[_selectedRange.value] = updatedSeries
        }
        _uiState.value = EntityDetailUiState.Loaded(updatedSeries, newState)
    }

    companion object {
        const val ARG_CONNECTION_ID = "connectionId"
        const val ARG_ENTITY_ID = "entityId"
    }
}

/**
 * A series is "plottable" only if it has at least two points with non-null
 * projected values. A single point can't form a line, and an all-null
 * series (e.g. every row was "unavailable") would render as an empty chart.
 */
private fun HistorySeries.hasPlottableData(): Boolean =
    points.count { it.value != null } >= 2
