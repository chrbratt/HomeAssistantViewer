package se.inix.homeassistantviewer.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.inix.homeassistantviewer.data.model.HaEntityState
import se.inix.homeassistantviewer.domain.history.HistoryRange
import se.inix.homeassistantviewer.ui.common.EditTextDialog
import se.inix.homeassistantviewer.ui.detail.components.HistoryChart
import se.inix.homeassistantviewer.ui.detail.components.TimeRangeChips

/**
 * Standalone screen accessed from a card's info-icon. Hosts the time-range
 * chips and the history chart. Composition follows the project convention
 * of one screen-level Composable per file, with reusable pieces extracted
 * to `components/`.
 *
 * **Layout is orientation-aware:**
 *  - Portrait → vertical stack with the chart expanding to fill all space
 *    below the value-card / range chips. No more wasted lower half.
 *  - Landscape → side-by-side: value-card + chips on the left, chart on
 *    the right (≈65 % of the width, full height). Both stay visible
 *    without any awkward clipping or scroll.
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
    val customName by viewModel.customName.collectAsStateWithLifecycle()

    var renaming by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentDisplayName(customName, uiState, viewModel.entityId),
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
                    IconButton(onClick = { renaming = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Rename")
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        EntityDetailBody(
            uiState = uiState,
            selectedRange = selectedRange,
            onSelectRange = viewModel::selectRange,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }

    if (renaming) {
        val haName = currentState(uiState)?.friendlyName ?: viewModel.entityId
        EditTextDialog(
            title = "Rename entity",
            label = "Display name",
            // Pre-fill with the *current* override (or HA's name if none), so
            // editing nudges the existing value rather than forcing a retype.
            initialValue = customName ?: haName,
            placeholder = haName,
            supportingText = "Leave empty to use the Home Assistant name (\"$haName\").",
            onConfirm = { name ->
                viewModel.setCustomName(name)
                renaming = false
            },
            onDismiss = { renaming = false }
        )
    }
}

/**
 * Picks the layout based on the available aspect ratio rather than
 * `Configuration.orientation`. This catches multi-window / split-screen
 * scenarios correctly — those don't change `Configuration.orientation`
 * but do change the area we actually have to work with.
 */
@Composable
private fun EntityDetailBody(
    uiState: EntityDetailUiState,
    selectedRange: HistoryRange,
    onSelectRange: (HistoryRange) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        // Wider than tall → side-by-side. The threshold deliberately uses
        // the visible area (after Scaffold padding) so it reacts the same
        // way for a phone in landscape and for a tablet in any split-mode.
        val sideBySide = maxWidth > maxHeight

        if (sideBySide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.40f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CurrentValueCard(state = currentState(uiState))
                    TimeRangeChips(selected = selectedRange, onSelect = onSelectRange)
                }
                ChartBody(
                    state = uiState,
                    range = selectedRange,
                    modifier = Modifier
                        .weight(0.60f)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CurrentValueCard(state = currentState(uiState))
                TimeRangeChips(selected = selectedRange, onSelect = onSelectRange)
                ChartBody(
                    state = uiState,
                    range = selectedRange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

/**
 * Renders one of: spinner / chart / empty / error, sized by [modifier].
 * The non-chart states share a centred box so swapping between e.g.
 * Loading and Loaded doesn't shift the layout around.
 */
@Composable
private fun ChartBody(
    state: EntityDetailUiState,
    range: HistoryRange,
    modifier: Modifier = Modifier
) {
    when (state) {
        is EntityDetailUiState.Loading -> CenteredProgress(modifier)
        is EntityDetailUiState.Loaded -> HistoryChart(
            series = state.series,
            range = range,
            modifier = modifier.fillMaxWidth()
        )
        is EntityDetailUiState.Empty -> MessageBox(
            modifier = modifier,
            text = "No history recorded for this entity in the selected range."
        )
        is EntityDetailUiState.Error -> MessageBox(
            modifier = modifier,
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
private fun CenteredProgress(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MessageBox(
    text: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
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

/**
 * Title resolution: user-defined override → HA's friendly_name → entityId.
 * Mirrors `cardDisplayTitle` on the dashboard so renaming an entity from
 * the detail screen is reflected immediately in both surfaces.
 */
private fun currentDisplayName(
    customName: String?,
    state: EntityDetailUiState,
    fallback: String
): String = customName?.takeIf { it.isNotBlank() }
    ?: currentState(state)?.friendlyName
    ?: fallback
