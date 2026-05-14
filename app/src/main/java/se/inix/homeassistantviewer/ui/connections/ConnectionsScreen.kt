package se.inix.homeassistantviewer.ui.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import se.inix.homeassistantviewer.data.model.HaConnection
import se.inix.homeassistantviewer.di.AppViewModelProvider
import se.inix.homeassistantviewer.ui.connections.components.ConnectionEditDialog
import se.inix.homeassistantviewer.ui.connections.components.ConnectionEditState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConnectionsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val connections by viewModel.connections.collectAsStateWithLifecycle()
    val addressResult by viewModel.addressResult.collectAsStateWithLifecycle()
    val apiResult by viewModel.apiResult.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTesting.collectAsStateWithLifecycle()

    var editState by remember { mutableStateOf<ConnectionEditState?>(null) }
    var deleteTarget by remember { mutableStateOf<HaConnection?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Connections") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.clearTestResults()
                editState = ConnectionEditState()
            }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add connection")
            }
        }
    ) { innerPadding ->
        if (connections.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No connections yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tap + to add your Home Assistant installation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(connections, key = { it.id }) { conn ->
                    ListItem(
                        headlineContent = { Text(conn.name) },
                        supportingContent = {
                            Text(
                                conn.baseUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = {
                                    viewModel.clearTestResults()
                                    editState = ConnectionEditState(conn.id, conn.name, conn.baseUrl, conn.token)
                                }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { deleteTarget = conn }) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    editState?.let { state ->
        ConnectionEditDialog(
            isNew = state.id == null,
            initial = state,
            addressResult = addressResult,
            apiResult = apiResult,
            isTesting = isTesting,
            urlValidator = viewModel::isValidUrl,
            onTest = { url, token -> viewModel.runConnectionTest(url, token) },
            onSave = { name, url, token ->
                val saved = if (state.id == null) {
                    viewModel.addConnection(name, url, token)
                } else {
                    viewModel.updateConnection(state.id, name, url, token)
                }
                if (saved) editState = null
                saved
            },
            onDismiss = { editState = null }
        )
    }

    deleteTarget?.let { conn ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Remove connection?") },
            text = {
                Text("\"${conn.name}\" and all its favorited entities will be removed from the dashboard.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteConnection(conn.id)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}
