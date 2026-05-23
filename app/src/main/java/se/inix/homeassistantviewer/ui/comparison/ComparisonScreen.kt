package se.inix.homeassistantviewer.ui.comparison

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.inix.homeassistantviewer.domain.history.HistoryExportFeedback
import se.inix.homeassistantviewer.domain.history.HistoryRange
import se.inix.homeassistantviewer.ui.common.rememberHistoryCsvExportLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(
    viewModel: ComparisonViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedRange by viewModel.selectedRange.collectAsStateWithLifecycle()
    val comparisonSelection by viewModel.comparisonSelection.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val launchExport = rememberHistoryCsvExportLauncher(
        suggestedFileName = { viewModel.suggestExportFileName().orEmpty() },
        onExportToUri = viewModel::exportToUri
    )
    val exportEnabled = uiState is ComparisonUiState.Loaded && viewModel.canExportCurrentRange()

    LaunchedEffect(viewModel) {
        viewModel.exportFeedbackEvents.collect { feedback ->
            val message = when (feedback) {
                is HistoryExportFeedback.Success -> feedback.message
                is HistoryExportFeedback.Error -> feedback.message
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Compare") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (comparisonSelection.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearSelection() }) {
                            Text("Clear")
                        }
                    }
                    if (uiState !is ComparisonUiState.EmptySelection) {
                        IconButton(
                            onClick = launchExport,
                            enabled = exportEnabled
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = "Export CSV")
                        }
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is ComparisonUiState.Loading -> Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            is ComparisonUiState.EmptySelection -> ComparisonEmptyState(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            )

            is ComparisonUiState.Error -> Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is ComparisonUiState.Loaded -> ComparisonLoadedBody(
                state = state,
                selectedRange = selectedRange,
                onSelectRange = viewModel::selectRange,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun ComparisonEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No entities selected",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Long-press a favorite on the dashboard to select entities for comparison.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
