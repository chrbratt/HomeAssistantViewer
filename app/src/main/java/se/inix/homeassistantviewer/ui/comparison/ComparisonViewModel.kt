package se.inix.homeassistantviewer.ui.comparison

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import se.inix.homeassistantviewer.data.model.ComparisonEntity
import se.inix.homeassistantviewer.data.model.FavoriteItem
import se.inix.homeassistantviewer.data.model.HaConnection
import se.inix.homeassistantviewer.data.model.HaEntityState
import se.inix.homeassistantviewer.data.model.HaHistoryRow
import se.inix.homeassistantviewer.domain.comparison.ComparisonChartGrouper
import se.inix.homeassistantviewer.domain.comparison.ComparisonChartGroup
import se.inix.homeassistantviewer.domain.comparison.ComparisonExcludedEntry
import se.inix.homeassistantviewer.domain.comparison.ComparisonSeriesEntry
import se.inix.homeassistantviewer.domain.history.HistoryCsvEncoder
import se.inix.homeassistantviewer.domain.history.HistoryEntityExport
import se.inix.homeassistantviewer.domain.history.HistoryExportFeedback
import se.inix.homeassistantviewer.domain.history.HistoryExportMetadata
import se.inix.homeassistantviewer.domain.history.HistoryRange
import se.inix.homeassistantviewer.domain.history.HistorySeriesBuilder
import se.inix.homeassistantviewer.domain.history.SeriesKind
import se.inix.homeassistantviewer.domain.history.suggestHistoryExportFileName
import java.io.IOException
import java.time.Instant

sealed class ComparisonUiState {
    data object Loading : ComparisonUiState()
    data object EmptySelection : ComparisonUiState()
    data class Loaded(
        val range: HistoryRange,
        val chartGroups: List<ComparisonChartGroup>,
        val binarySeries: List<ComparisonSeriesEntry>,
        val categoricalSeries: List<ComparisonSeriesEntry>,
        val excluded: List<ComparisonExcludedEntry>
    ) : ComparisonUiState()

    data class Error(val message: String) : ComparisonUiState()
}

class ComparisonViewModel(
    private val dataSource: ComparisonHistoryDataSource,
    comparisonSelection: StateFlow<Set<ComparisonEntity>>,
    private val favorites: StateFlow<List<FavoriteItem>>,
    private val connections: StateFlow<List<HaConnection>> = MutableStateFlow(emptyList()),
    private val clearComparisonSelection: () -> Unit = {},
    private val now: () -> Instant = Instant::now
) : ViewModel() {

    val comparisonSelection: StateFlow<Set<ComparisonEntity>> = comparisonSelection

    private val _selectedRange = MutableStateFlow(HistoryRange.Default)
    val selectedRange: StateFlow<HistoryRange> = _selectedRange.asStateFlow()

    private val _uiState = MutableStateFlow<ComparisonUiState>(ComparisonUiState.Loading)
    val uiState: StateFlow<ComparisonUiState> = _uiState.asStateFlow()

    private val _exportFeedbackEvents = MutableSharedFlow<HistoryExportFeedback>(extraBufferCapacity = 8)
    val exportFeedbackEvents: SharedFlow<HistoryExportFeedback> = _exportFeedbackEvents.asSharedFlow()

    private data class RangeCache(
        val uiState: ComparisonUiState,
        val exportMetadata: HistoryExportMetadata,
        val exportEntities: List<HistoryEntityExport>
    )

    private val rangeCache = mutableMapOf<HistoryRange, RangeCache>()
    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            comparisonSelection.collect { selection ->
                rangeCache.clear()
                if (selection.isEmpty()) {
                    _uiState.value = ComparisonUiState.EmptySelection
                } else {
                    fetchForRange(_selectedRange.value)
                }
            }
        }
    }

    fun selectRange(range: HistoryRange) {
        if (_selectedRange.value == range) return
        _selectedRange.value = range
        fetchForRange(range)
    }

    fun refresh() {
        rangeCache.remove(_selectedRange.value)
        fetchForRange(_selectedRange.value)
    }

    fun clearSelection() {
        clearComparisonSelection()
    }

    fun canExportCurrentRange(): Boolean = _uiState.value is ComparisonUiState.Loaded

    fun suggestExportFileName(): String? {
        if (!canExportCurrentRange()) return null
        val count = comparisonSelection.value.size
        return suggestHistoryExportFileName(
            prefix = "comparison-$count-entities",
            range = _selectedRange.value
        )
    }

    fun exportToUri(contentResolver: ContentResolver, uri: Uri) {
        if (!canExportCurrentRange()) return
        viewModelScope.launch {
            val range = _selectedRange.value
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val cache = buildExportCache(range)
                    rangeCache[range]?.let { existing ->
                        rangeCache[range] = existing.copy(
                            exportMetadata = cache.exportMetadata,
                            exportEntities = cache.exportEntities
                        )
                    }
                    val bytes = HistoryCsvEncoder.encode(cache.exportMetadata, cache.exportEntities)
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(bytes)
                    } ?: throw IOException("Could not open file for writing")
                    cache
                }
            }
            result.fold(
                onSuccess = { cache ->
                    val rows = HistoryCsvEncoder.rowCount(cache.exportMetadata, cache.exportEntities)
                    postExportFeedback(
                        HistoryExportFeedback.Success(
                            "Exported ${cache.exportMetadata.range.label} ($rows rows)."
                        )
                    )
                },
                onFailure = {
                    postExportFeedback(
                        HistoryExportFeedback.Error(
                            it.message?.takeIf { msg -> msg.isNotBlank() } ?: "Export failed"
                        )
                    )
                }
            )
        }
    }

    private fun postExportFeedback(feedback: HistoryExportFeedback) {
        viewModelScope.launch { _exportFeedbackEvents.emit(feedback) }
    }

    private fun fetchForRange(range: HistoryRange) {
        val selection = comparisonSelection.value
        if (selection.isEmpty()) {
            _uiState.value = ComparisonUiState.EmptySelection
            return
        }

        rangeCache[range]?.let { cached ->
            _uiState.value = cached.uiState
            return
        }

        _uiState.value = ComparisonUiState.Loading
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            val end = now()
            val start = end.minus(range.duration)
            val favorites = favorites.value
            val result = runCatching {
                coroutineScope {
                    selection.map { entity ->
                        async {
                            val rows = dataSource.getHistory(
                                entity.connectionId,
                                entity.entityId,
                                start,
                                end
                            )
                            val current = dataSource.getCurrentState(
                                entity.connectionId,
                                entity.entityId
                            )
                            toFetchResult(entity, rows, current, favorites)
                        }
                    }.map { it.await() }
                }
            }

            result.onSuccess { results ->
                val entries = results.map { it.chartEntry }
                val exportEntities = results.map { it.exportEntry }
                val grouped = ComparisonChartGrouper.group(entries)
                val state = ComparisonUiState.Loaded(
                    range = range,
                    chartGroups = grouped.chartGroups,
                    binarySeries = grouped.binarySeries,
                    categoricalSeries = grouped.categoricalSeries,
                    excluded = grouped.excluded
                )
                rangeCache[range] = RangeCache(
                    uiState = state,
                    exportMetadata = HistoryExportMetadata(
                        exportedAt = end,
                        range = range,
                        rangeStart = start,
                        rangeEnd = end
                    ),
                    exportEntities = exportEntities
                )
                _uiState.value = state
            }.onFailure { t ->
                _uiState.value = ComparisonUiState.Error(
                    t.message?.takeIf { it.isNotBlank() } ?: "Failed to load comparison history"
                )
            }
        }
    }

    private suspend fun buildExportCache(
        range: HistoryRange,
        start: Instant = now().minus(range.duration),
        end: Instant = now()
    ): RangeCache {
        val selection = comparisonSelection.value
        val favorites = favorites.value
        val results = coroutineScope {
            selection.map { entity ->
                async {
                    val rows = dataSource.getHistory(entity.connectionId, entity.entityId, start, end)
                    val current = dataSource.getCurrentState(entity.connectionId, entity.entityId)
                    toFetchResult(entity, rows, current, favorites)
                }
            }.map { it.await() }
        }
        return RangeCache(
            uiState = rangeCache[range]?.uiState
                ?: ComparisonUiState.Loaded(range, emptyList(), emptyList(), emptyList(), emptyList()),
            exportMetadata = HistoryExportMetadata(
                exportedAt = end,
                range = range,
                rangeStart = start,
                rangeEnd = end
            ),
            exportEntities = results.map { it.exportEntry }
        )
    }

    private fun toFetchResult(
        entity: ComparisonEntity,
        rows: List<HaHistoryRow>,
        current: HaEntityState?,
        favorites: List<FavoriteItem>
    ): ComparisonFetchResult {
        val domain = current?.entityId?.substringBefore('.')
            ?: entity.entityId.substringBefore('.')
        val unit = current?.unitOfMeasurement
        val series = HistorySeriesBuilder.build(rows, domain, unit)
        val fullSeries = HistorySeriesBuilder.buildFull(rows, domain, unit)
        val displayName = resolveDisplayName(entity, favorites, current)
        return ComparisonFetchResult(
            chartEntry = ComparisonSeriesEntry(
                connectionId = entity.connectionId,
                entityId = entity.entityId,
                displayName = displayName,
                series = series,
                unit = (series.kind as? SeriesKind.Numeric)?.unit,
                colorIndex = 0
            ),
            exportEntry = HistoryEntityExport(
                connectionId = entity.connectionId,
                connectionName = resolveConnectionName(entity.connectionId),
                entityId = entity.entityId,
                displayName = displayName,
                series = fullSeries
            )
        )
    }

    private fun resolveConnectionName(connectionId: String): String =
        connections.value.firstOrNull { it.id == connectionId }?.name ?: connectionId

    private fun resolveDisplayName(
        entity: ComparisonEntity,
        favorites: List<FavoriteItem>,
        current: HaEntityState?
    ): String {
        val customName = favorites
            .filterIsInstance<FavoriteItem.Entity>()
            .firstOrNull { it.connectionId == entity.connectionId && it.entityId == entity.entityId }
            ?.customName
        return customName?.takeIf { it.isNotBlank() }
            ?: current?.friendlyName?.takeIf { it.isNotBlank() }
            ?: entity.entityId
    }

    private data class ComparisonFetchResult(
        val chartEntry: ComparisonSeriesEntry,
        val exportEntry: HistoryEntityExport
    )
}
