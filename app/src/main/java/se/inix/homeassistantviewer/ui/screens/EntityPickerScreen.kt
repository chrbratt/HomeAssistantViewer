package se.inix.homeassistantviewer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import se.inix.homeassistantviewer.viewmodel.AppViewModelProvider
import se.inix.homeassistantviewer.viewmodel.EntityPickerUiState
import se.inix.homeassistantviewer.viewmodel.EntityPickerViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EntityPickerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EntityPickerViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val connections by viewModel.connections.collectAsStateWithLifecycle()
    val selectedConnectionId by viewModel.selectedConnectionId.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Manage Favorites") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadEntities) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Reload entities")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // ── Connection tabs (only when > 1 connection) ─────────────────────
            if (connections.size > 1) {
                val selectedIndex = connections.indexOfFirst { it.id == selectedConnectionId }
                    .coerceAtLeast(0)
                PrimaryScrollableTabRow(selectedTabIndex = selectedIndex) {
                    connections.forEachIndexed { index, conn ->
                        Tab(
                            selected = index == selectedIndex,
                            onClick = { viewModel.selectConnection(conn.id) },
                            text = { Text(conn.name) }
                        )
                    }
                }
            }

            // ── Search ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search by name or entity ID…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors()
            )

            when (val state = uiState) {
                is EntityPickerUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }

                is EntityPickerUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Failed to load entities",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(state.message, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = viewModel::loadEntities) { Text("Retry") }
                    }
                }

                is EntityPickerUiState.Success -> {
                    // ── Favorites badge ────────────────────────────────────────
                    val favCount = state.favoriteEntityIds.size
                    if (favCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Star, null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "$favCount ${if (favCount == 1) "entity" else "entities"} selected",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // ── Category filter chips ──────────────────────────────────
                    if (state.availableCategories.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = categoryFilter == null,
                                    onClick = { viewModel.categoryFilter.value = null },
                                    label = { Text("All") }
                                )
                            }
                            items(state.availableCategories) { category ->
                                FilterChip(
                                    selected = categoryFilter == category,
                                    onClick = {
                                        viewModel.categoryFilter.value =
                                            if (categoryFilter == category) null else category
                                    },
                                    label = { Text(domainDisplayName(category)) }
                                )
                            }
                        }
                    }

                    if (state.groupedEntities.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No entities match your search",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            state.groupedEntities.forEach { (domain, entities) ->
                                stickyHeader(key = "header_$domain") {
                                    DomainSectionHeader(domain, entities.size)
                                }
                                items(entities, key = { it.entityId }) { entity ->
                                    val isFavorite = entity.entityId in state.favoriteEntityIds
                                    ListItem(
                                        headlineContent = {
                                            Text(entity.friendlyName ?: entity.entityId)
                                        },
                                        supportingContent = {
                                            Text(
                                                entity.entityId,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                getIconForDomain(domain),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        trailingContent = {
                                            IconButton(onClick = {
                                                viewModel.toggleFavorite(entity.entityId)
                                            }) {
                                                Icon(
                                                    imageVector = if (isFavorite) Icons.Rounded.Star
                                                    else Icons.Rounded.StarBorder,
                                                    contentDescription = if (isFavorite) "Remove"
                                                    else "Add to favorites",
                                                    tint = if (isFavorite)
                                                        MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        colors = ListItemDefaults.colors(
                                            containerColor = if (isFavorite)
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 56.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
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

@Composable
private fun DomainSectionHeader(domain: String, entityCount: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                getIconForDomain(domain), contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                domainDisplayName(domain),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                "$entityCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun domainDisplayName(domain: String): String = when (domain) {
    "light" -> "Lights"
    "switch" -> "Switches"
    "sensor" -> "Sensors"
    "binary_sensor" -> "Binary Sensors"
    "climate" -> "Climate"
    "cover" -> "Covers"
    "fan" -> "Fans"
    "lock" -> "Locks"
    "media_player" -> "Media Players"
    "input_boolean" -> "Input Booleans"
    "automation" -> "Automations"
    "scene" -> "Scenes"
    "script" -> "Scripts"
    "weather" -> "Weather"
    "camera" -> "Cameras"
    "person" -> "Persons"
    "device_tracker" -> "Device Trackers"
    else -> domain.replace("_", " ").replaceFirstChar { it.uppercase() }
}
