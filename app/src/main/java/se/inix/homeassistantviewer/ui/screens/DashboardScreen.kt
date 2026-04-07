package se.inix.homeassistantviewer.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import se.inix.homeassistantviewer.data.ConnectionState
import se.inix.homeassistantviewer.data.model.HaEntityState
import se.inix.homeassistantviewer.ui.theme.HomeAssistantStugaTheme
import se.inix.homeassistantviewer.viewmodel.AppViewModelProvider
import se.inix.homeassistantviewer.viewmodel.DashboardEntity
import se.inix.homeassistantviewer.viewmodel.DashboardUiState
import se.inix.homeassistantviewer.viewmodel.DashboardViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToEntityPicker: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dashboardColumns by viewModel.dashboardColumns.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Home Assistant Viewer") },
                actions = {
                    // Hide entity picker when no connections exist — there is nothing to pick from.
                    if (uiState !is DashboardUiState.NoConnections) {
                        IconButton(onClick = onNavigateToEntityPicker) {
                            Icon(Icons.Rounded.Star, contentDescription = "Manage favorites")
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        ConnectionDot(connectionState = connectionState) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is DashboardUiState.NoConnections -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CloudOff, contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "No connection configured",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Add your Home Assistant connection in Settings before selecting entities to show here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = onNavigateToSettings) { Text("Go to Settings") }
                    }
                }

                is DashboardUiState.NoFavorites -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Rounded.StarBorder, contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("No favorites selected", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tap the star icon to choose which entities to show on the dashboard.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onNavigateToEntityPicker) { Text("Select favorites") }
                    }
                }

                is DashboardUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Failed to load dashboard",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { viewModel.fetchStates() }) { Text("Retry") }
                        OutlinedButton(onClick = onNavigateToSettings) { Text("Go to Settings") }
                    }
                }

                is DashboardUiState.Success -> {
                    val lazyGridState = rememberLazyStaggeredGridState()

                    // Local copy of the entity list — updated immediately on drag for smooth
                    // animation, then saved to settings when the user lifts their finger.
                    var localItems by remember { mutableStateOf(state.entities) }

                    // Keep local list in sync with ViewModel whenever it changes,
                    // but only while nobody is dragging to avoid jitter.
                    val reorderState = rememberReorderableLazyStaggeredGridState(lazyGridState) { from, to ->
                        localItems = localItems.toMutableList().apply {
                            add(to.index, removeAt(from.index))
                        }
                    }

                    LaunchedEffect(state.entities) {
                        if (!reorderState.isAnyItemDragging) {
                            localItems = state.entities
                        }
                    }

                    val haptic = LocalHapticFeedback.current

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyVerticalStaggeredGrid(
                            state = lazyGridState,
                            columns = StaggeredGridCells.Fixed(dashboardColumns),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalItemSpacing = 10.dp,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                localItems,
                                key = { "${it.connectionId}/${it.entityId}" }
                            ) { item ->
                                ReorderableItem(
                                    state = reorderState,
                                    key = "${item.connectionId}/${item.entityId}"
                                ) { isDragging ->
                                    val elevation by animateDpAsState(
                                        if (isDragging) 10.dp else 0.dp,
                                        label = "dragElevation"
                                    )
                                    val scale by animateFloatAsState(
                                        if (isDragging) 1.04f else 1f,
                                        label = "dragScale"
                                    )

                                    EntityCard(
                                        item = item,
                                        onAction = { action -> viewModel.performAction(action) },
                                        modifier = Modifier
                                            .longPressDraggableHandle(
                                                onDragStarted = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDragStopped = {
                                                    viewModel.saveFavoriteOrder(localItems)
                                                }
                                            )
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                            .shadow(elevation, shape = androidx.compose.material3.MaterialTheme.shapes.large)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Connection dot indicator ──────────────────────────────────────────────────

@Composable
private fun ConnectionDot(connectionState: ConnectionState, content: @Composable () -> Unit) {
    val dotColor by animateColorAsState(
        targetValue = when (connectionState) {
            is ConnectionState.Connected -> Color(0xFF4CAF50)
            is ConnectionState.Connecting -> Color(0xFFFF9800)
            else -> MaterialTheme.colorScheme.error
        },
        label = "connectionDotColor"
    )
    Box {
        content()
        Box(
            modifier = Modifier
                .size(8.dp)
                .align(Alignment.TopEnd)
                .background(dotColor, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun EntityCardPreview() {
    HomeAssistantStugaTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EntityCard(
                item = DashboardEntity(
                    connectionId = "default",
                    entityId = "light.living_room",
                    entity = HaEntityState(
                        entityId = "light.living_room",
                        state = "on",
                        attributes = mapOf(
                            "friendly_name" to "Kök - Taklampa",
                            "brightness" to 40.8
                        ),
                        lastChanged = "",
                        lastUpdated = ""
                    )
                ),
                onAction = {}
            )
            EntityCard(
                item = DashboardEntity(
                    connectionId = "default",
                    entityId = "sensor.outdoor_temp",
                    entity = HaEntityState(
                        entityId = "sensor.outdoor_temp",
                        state = "4.7",
                        attributes = mapOf(
                            "friendly_name" to "Outdoor Temperature",
                            "device_class" to "temperature",
                            "unit_of_measurement" to "°C"
                        ),
                        lastChanged = "",
                        lastUpdated = ""
                    )
                ),
                onAction = {}
            )
            EntityCard(
                item = DashboardEntity(
                    connectionId = "default",
                    entityId = "sensor.wind_speed",
                    entity = HaEntityState(
                        entityId = "sensor.wind_speed",
                        state = "2.30555555555556",
                        attributes = mapOf(
                            "friendly_name" to "WeatherStation Wind Speed",
                            "device_class" to "wind_speed",
                            "unit_of_measurement" to "m/s"
                        ),
                        lastChanged = "",
                        lastUpdated = ""
                    )
                ),
                onAction = {}
            )
            EntityCard(
                item = DashboardEntity(
                    connectionId = "default",
                    entityId = "binary_sensor.motion",
                    entity = HaEntityState(
                        entityId = "binary_sensor.motion",
                        state = "on",
                        attributes = mapOf(
                            "friendly_name" to "Entrance - Motion",
                            "device_class" to "motion"
                        ),
                        lastChanged = "",
                        lastUpdated = ""
                    )
                ),
                onAction = {}
            )
            EntityCard(
                item = DashboardEntity(
                    connectionId = "default",
                    entityId = "sensor.unavailable",
                    entity = null
                ),
                onAction = {}
            )
        }
    }
}
