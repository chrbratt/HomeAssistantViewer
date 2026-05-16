package se.inix.homeassistantviewer.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import se.inix.homeassistantviewer.R
import se.inix.homeassistantviewer.di.AppViewModelProvider
import se.inix.homeassistantviewer.ui.dashboard.components.DashboardErrorView
import se.inix.homeassistantviewer.ui.dashboard.components.DashboardGrid
import se.inix.homeassistantviewer.ui.dashboard.components.DashboardStatusBanner
import se.inix.homeassistantviewer.ui.dashboard.components.NoConnectionsEmpty
import se.inix.homeassistantviewer.ui.dashboard.components.NoFavoritesEmpty
import se.inix.homeassistantviewer.ui.dashboard.components.RemoveItemDialog
import se.inix.homeassistantviewer.ui.dashboard.components.SettingsHealthBadge

/**
 * The dashboard is intentionally a thin orchestrator:
 *  - it does no business logic
 *  - it delegates state derivation to [DashboardViewModel]
 *  - it delegates everything visual to per-state composables in `components/` / `cards/`
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToEntityPicker: () -> Unit,
    onNavigateToEntityDetail: (connectionId: String, entityId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dashboardColumns by viewModel.dashboardColumns.collectAsStateWithLifecycle()
    val statusBar by viewModel.statusBar.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var pendingRemoval by remember { mutableStateOf<DashboardItem?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_app_logo),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Text("HA Viewer")
                    }
                },
                actions = {
                    if (uiState !is DashboardUiState.NoConnections) {
                        IconButton(onClick = onNavigateToEntityPicker) {
                            Icon(Icons.Rounded.Star, contentDescription = "Manage favorites")
                        }
                    }
                    SettingsHealthBadge(
                        health = statusBar.toConnectionHealth(),
                        onClick = onNavigateToSettings
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            DashboardStatusBanner(
                status = statusBar,
                onOpenSettings = onNavigateToSettings
            )

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (val state = uiState) {
                        is DashboardUiState.Loading ->
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                        is DashboardUiState.NoConnections ->
                            NoConnectionsEmpty(onNavigateToSettings)

                        is DashboardUiState.NoFavorites ->
                            NoFavoritesEmpty(onNavigateToEntityPicker)

                        is DashboardUiState.Error -> DashboardErrorView(
                            message = state.message,
                            onRetry = { viewModel.fetchStates() },
                            onSettings = onNavigateToSettings
                        )

                        is DashboardUiState.Success -> DashboardGrid(
                            items = state.items,
                            columns = dashboardColumns,
                            onAction = viewModel::performAction,
                            onSaveOrder = viewModel::saveItemOrder,
                            onRequestRemove = { pendingRemoval = it },
                            onOpenDetail = onNavigateToEntityDetail
                        )
                    }
                }
            }
        }
    }

    pendingRemoval?.let { item ->
        RemoveItemDialog(
            item = item,
            onConfirm = {
                viewModel.removeItem(item)
                pendingRemoval = null
            },
            onDismiss = { pendingRemoval = null }
        )
    }
}
