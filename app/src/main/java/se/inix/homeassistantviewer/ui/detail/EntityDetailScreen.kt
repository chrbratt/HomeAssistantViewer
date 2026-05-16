package se.inix.homeassistantviewer.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.inix.homeassistantviewer.data.model.HaEntityState
import se.inix.homeassistantviewer.ui.detail.components.HistoryChart
import se.inix.homeassistantviewer.ui.detail.components.TimeRangeChips

/**
 * Standalone screen accessed from a card's info-icon. Hosts the time-range
 * chips and the Vico chart. Composition follows the project convention of
 * one screen-level Composable per file, with reusable pieces extracted to
 * `components/`.
 *
 * The screen never opens a WebSocket itself — it reuses the live flow the
 * detail view-model already subscribes to, so backing out releases all
 * detail-specific state without leaking sockets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntityDetailScreen(
    viewModel: EntityDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedRange by viewModel.selectedRange.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentDisplayName(uiState, viewModel.entityId),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CurrentValueCard(state = currentState(uiState))
            TimeRangeChips(selected = selectedRange, onSelect = viewModel::selectRange)
            ChartBody(state = uiState, range = selectedRange)
        }
    }
}

/**
 * The body either shows the chart, a spinner, an empty-state message, or
 * an error retry hint depending on the current ui state. Pulled out so
 * the top-level screen Composable stays close to layout, not branching.
 */
@Composable
private fun ChartBody(state: EntityDetailUiState, range: se.inix.homeassistantviewer.domain.history.HistoryRange) {
    when (state) {
        is EntityDetailUiState.Loading -> CenteredProgress()
        is EntityDetailUiState.Loaded -> HistoryChart(series = state.series, range = range)
        is EntityDetailUiState.Empty -> EmptyMessage(
            "No history recorded for this entity in the selected range."
        )
        is EntityDetailUiState.Error -> EmptyMessage(
            text = state.message,
            highlight = true
        )
    }
}

@Composable
private fun CurrentValueCard(state: HaEntityState?) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Current value",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = state?.let { formatValue(it) } ?: "—",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            state?.lastChanged?.let { lastChanged ->
                Text(
                    text = "Updated $lastChanged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CenteredProgress() {
    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyMessage(text: String, highlight: Boolean = false) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (highlight) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatValue(state: HaEntityState): String {
    val unit = state.unitOfMeasurement
    return if (unit.isNullOrBlank()) state.state else "${state.state} $unit"
}

private fun currentState(state: EntityDetailUiState): HaEntityState? = when (state) {
    is EntityDetailUiState.Loaded -> state.currentState
    is EntityDetailUiState.Empty -> state.currentState
    else -> null
}

private fun currentDisplayName(state: EntityDetailUiState, fallback: String): String =
    currentState(state)?.friendlyName ?: fallback
