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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Blinds
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Toys
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
                title = { Text("HA Viewer") },
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
                                        onToggle = {
                                            viewModel.toggleEntity(item.connectionId, item.entityId)
                                        },
                                        onSetBrightness = { pct ->
                                            viewModel.setBrightness(item.connectionId, item.entityId, pct)
                                        },
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

// ── Entity card dispatcher ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityCard(
    item: DashboardEntity,
    onToggle: () -> Unit,
    onSetBrightness: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val entity = item.entity
    if (entity == null) {
        UnavailableEntityCard(entityId = item.entityId, modifier = modifier)
        return
    }

    val isInteractive = entity.domain == "light" || entity.domain == "switch"
    if (isInteractive) {
        ControlCard(entity = entity, onToggle = onToggle, onSetBrightness = onSetBrightness, modifier = modifier)
    } else {
        SensorCard(entity = entity, modifier = modifier)
    }
}

// ── Control card (light / switch) ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlCard(
    entity: HaEntityState,
    onToggle: () -> Unit,
    onSetBrightness: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isOn = entity.state == "on"

    val containerColor by animateColorAsState(
        targetValue = if (isOn) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = spring(),
        label = "controlCardBg"
    )
    val onContainerColor = if (isOn) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant

    var isDragging by remember { mutableStateOf(false) }
    var localBrightness by remember(entity.entityId) {
        mutableFloatStateOf((entity.brightnessPercent ?: 100).toFloat())
    }
    val remoteBrightness = entity.brightnessPercent?.toFloat()
    if (!isDragging && remoteBrightness != null) localBrightness = remoteBrightness

    Card(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Icon row + switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isOn) onContainerColor.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconForEntity(entity),
                        contentDescription = null,
                        tint = onContainerColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Switch(checked = isOn, onCheckedChange = { onToggle() })
            }

            // Name + brightness %
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = entity.friendlyName ?: entity.entityId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onContainerColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isOn && entity.supportsBrightness) {
                    Text(
                        text = "${localBrightness.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = onContainerColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                    )
                }
            }

            // State text
            Text(
                text = if (isOn) "On" else "Off",
                style = MaterialTheme.typography.bodySmall,
                color = onContainerColor.copy(alpha = 0.6f)
            )

            // Brightness slider
            if (isOn && entity.supportsBrightness) {
                Slider(
                    value = localBrightness,
                    onValueChange = { isDragging = true; localBrightness = it },
                    onValueChangeFinished = { isDragging = false; onSetBrightness(localBrightness.toInt()) },
                    valueRange = 1f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ── Sensor card (read-only) ───────────────────────────────────────────────────

/**
 * Two-row layout so long names are never truncated:
 *
 *   WeatherStation Wind Speed    ← full width, up to 2 lines
 *
 *   [🌬️]                  1.2  ← icon left, value right
 *                           m/s
 */
@Composable
private fun SensorCard(entity: HaEntityState, modifier: Modifier = Modifier) {
    val icon = getIconForEntity(entity)
    val iconTint = sensorIconTint(entity)
    val unit = entity.unitOfMeasurement
    val isBinary = entity.domain == "binary_sensor" && unit == null &&
        (entity.state == "on" || entity.state == "off")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Name — full card width, wraps to 2 lines if needed
            Text(
                text = entity.friendlyName ?: entity.entityId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Icon left · value right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconTint.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                when {
                    isBinary -> BinaryStateIndicator(isActive = entity.state == "on")
                    unit != null -> NumericValueDisplay(value = entity.state, unit = unit)
                    else -> Text(
                        text = formatSensorValue(entity.state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun NumericValueDisplay(value: String, unit: String) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = formatSensorValue(value),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 3.dp)
        )
    }
}

@Composable
private fun BinaryStateIndicator(isActive: Boolean) {
    val dotColor = if (isActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val label = if (isActive) "Active" else "Clear"
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Unavailable card ──────────────────────────────────────────────────────────

@Composable
private fun UnavailableEntityCard(entityId: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.CloudOff, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entityId.substringAfterLast(".").replace("_", " ")
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Unavailable",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ── Value formatting ──────────────────────────────────────────────────────────

/**
 * Rounds any numeric value to at most 1 decimal place.
 * "2.30555555555556" → "2.3", "116.0" → "116", "4.7" → "4.7", "on" → "On"
 */
private fun formatSensorValue(state: String): String {
    val d = state.toDoubleOrNull() ?: return state.replaceFirstChar { it.uppercase() }
    return if (d % 1.0 == 0.0) d.toLong().toString() else "%.1f".format(d)
}

// ── Icon resolution ───────────────────────────────────────────────────────────

/**
 * Resolves a semantic icon from [device_class] attribute first, then falls back to domain.
 * This gives temperature sensors a thermometer, wind sensors an air icon, etc.
 */
fun getIconForEntity(entity: HaEntityState): ImageVector {
    val dc = entity.attributes?.get("device_class") as? String
    return when {
        entity.domain == "light" -> Icons.Rounded.Lightbulb
        entity.domain == "switch" -> Icons.Rounded.Power
        entity.domain == "climate" -> Icons.Rounded.Thermostat
        entity.domain == "cover" -> Icons.Rounded.Blinds
        entity.domain == "fan" -> Icons.Rounded.Toys
        entity.domain == "lock" -> Icons.Rounded.Lock
        entity.domain == "media_player" -> Icons.Rounded.PlayArrow
        entity.domain == "weather" -> Icons.Rounded.WbCloudy
        dc == "temperature" -> Icons.Rounded.Thermostat
        dc == "humidity" || dc == "moisture" -> Icons.Rounded.WaterDrop
        dc == "wind_speed" || dc == "wind_direction" -> Icons.Rounded.Air
        dc == "illuminance" -> Icons.Rounded.WbSunny
        dc == "battery" -> Icons.Rounded.BatteryFull
        dc == "power" || dc == "energy" || dc == "voltage" || dc == "current" -> Icons.Rounded.Bolt
        dc == "motion" || dc == "occupancy" || dc == "presence" -> Icons.AutoMirrored.Rounded.DirectionsWalk
        else -> Icons.Rounded.Sensors
    }
}

/**
 * Domain-only icon lookup — used by [EntityPickerScreen] which has no entity attributes.
 */
fun getIconForDomain(domain: String): ImageVector = when (domain) {
    "light" -> Icons.Rounded.Lightbulb
    "switch" -> Icons.Rounded.Power
    "climate" -> Icons.Rounded.Thermostat
    "cover" -> Icons.Rounded.Blinds
    "fan" -> Icons.Rounded.Toys
    "lock" -> Icons.Rounded.Lock
    "media_player" -> Icons.Rounded.PlayArrow
    "weather" -> Icons.Rounded.WbCloudy
    "sensor" -> Icons.Rounded.Sensors
    "binary_sensor" -> Icons.Rounded.Sensors
    else -> Icons.Rounded.Sensors
}

// ── Temperature-aware icon tinting ────────────────────────────────────────────

/**
 * Returns a tint color for sensor icons.
 * Temperature sensors transition from icy blue → cool cyan → mild green → warm amber → hot red.
 * Other sensor types get a characteristic color based on device_class.
 */
@Composable
private fun sensorIconTint(entity: HaEntityState): Color {
    val dc = entity.attributes?.get("device_class") as? String
    val numericValue = entity.state.toDoubleOrNull()

    if (dc == "temperature" && numericValue != null) {
        return when {
            numericValue < 0 -> Color(0xFF90CAF9)    // icy blue
            numericValue < 8 -> Color(0xFF80DEEA)    // cool cyan
            numericValue < 18 -> Color(0xFFA5D6A7)   // mild green
            numericValue < 26 -> Color(0xFFFFCC80)   // warm amber
            else -> Color(0xFFEF9A9A)                // hot red
        }
    }

    return when (dc) {
        "humidity", "moisture" -> Color(0xFF81D4FA)
        "wind_speed", "wind_direction" -> Color(0xFF80CBC4)
        "illuminance" -> Color(0xFFFFF176)
        "battery" -> Color(0xFFA5D6A7)
        "power", "energy", "voltage", "current" -> Color(0xFFFFCC80)
        "motion", "occupancy", "presence" ->
            if (entity.state == "on") Color(0xFFFFB74D) else Color(0xFF9E9E9E)
        else -> MaterialTheme.colorScheme.primary
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
                onToggle = {}
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
                onToggle = {}
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
                onToggle = {}
            )
            EntityCard(
                item = DashboardEntity(
                    connectionId = "default",
                    entityId = "binary_sensor.motion",
                    entity = HaEntityState(
                        entityId = "binary_sensor.motion",
                        state = "on",
                        attributes = mapOf(
                            "friendly_name" to "Hall - Rörelsesensor",
                            "device_class" to "motion"
                        ),
                        lastChanged = "",
                        lastUpdated = ""
                    )
                ),
                onToggle = {}
            )
            EntityCard(
                item = DashboardEntity(
                    connectionId = "default",
                    entityId = "sensor.unavailable",
                    entity = null
                ),
                onToggle = {}
            )
        }
    }
}
