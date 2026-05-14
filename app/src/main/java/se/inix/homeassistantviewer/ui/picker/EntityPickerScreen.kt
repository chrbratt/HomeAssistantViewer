package se.inix.homeassistantviewer.ui.picker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldLineLimits
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
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import se.inix.homeassistantviewer.di.AppViewModelProvider
import se.inix.homeassistantviewer.ui.common.domainDisplayName
import se.inix.homeassistantviewer.ui.common.getIconForDomain
import se.inix.homeassistantviewer.ui.picker.components.DomainSectionHeader
import se.inix.homeassistantviewer.ui.picker.components.FavoritesActionsRow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EntityPickerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EntityPickerViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()
    val connections by viewModel.connections.collectAsStateWithLifecycle()
    val selectedConnectionId by viewModel.selectedConnectionId.collectAsStateWithLifecycle()

    val isLoading = uiState is EntityPickerUiState.Loading
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
                    IconButton(
                        onClick = viewModel::loadEntities,
                        enabled = !isLoading && connections.isNotEmpty()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Reload entities")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (connections.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Add a Home Assistant connection in Settings before picking entities.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Column
            }

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

            OutlinedTextField(
                state = viewModel.searchQuery,
                placeholder = { Text("Search by name or entity ID…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (viewModel.searchQuery.text.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearSearch) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                lineLimits = TextFieldLineLimits.SingleLine,
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = OutlinedTextFieldDefaults.contentPadding(top = 8.dp, bottom = 8.dp),
                colors = OutlinedTextFieldDefaults.colors()
            )

            when (val state = uiState) {
                is EntityPickerUiState.Loading -> Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is EntityPickerUiState.Error -> Column(
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

                is EntityPickerUiState.Success -> {
                    FavoritesActionsRow(
                        favCount = state.favoriteEntityIds.size,
                        onAddRowBreak = {
                            viewModel.addDivider()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Row break added to the dashboard")
                            }
                        }
                    )

                    if (state.availableCategories.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.favoriteEntityIds.isNotEmpty()) {
                                item {
                                    FilterChip(
                                        selected = showFavoritesOnly,
                                        onClick = {
                                            viewModel.showFavoritesOnly.value = !showFavoritesOnly
                                        },
                                        label = { Text("Favorites") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.Star,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                            item {
                                FilterChip(
                                    selected = categoryFilter == null && !showFavoritesOnly,
                                    onClick = {
                                        viewModel.categoryFilter.value = null
                                        viewModel.showFavoritesOnly.value = false
                                    },
                                    label = { Text("All") }
                                )
                            }
                            items(state.availableCategories) { category ->
                                FilterChip(
                                    selected = categoryFilter == category,
                                    onClick = {
                                        viewModel.categoryFilter.value =
                                            if (categoryFilter == category) null else category
                                        viewModel.showFavoritesOnly.value = false
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
