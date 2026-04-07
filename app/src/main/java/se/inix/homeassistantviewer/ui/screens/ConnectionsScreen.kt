package se.inix.homeassistantviewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import se.inix.homeassistantviewer.data.AddressProbeResult
import se.inix.homeassistantviewer.data.ApiProbeResult
import se.inix.homeassistantviewer.data.model.HaConnection
import se.inix.homeassistantviewer.viewmodel.AppViewModelProvider
import se.inix.homeassistantviewer.viewmodel.ConnectionsViewModel

private data class EditState(
    val id: String? = null,       // null = new connection
    val name: String = "",
    val baseUrl: String = "",
    val token: String = ""
)

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

    var editState by remember { mutableStateOf<EditState?>(null) }
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
                editState = EditState()
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
                Text(
                    "No connections yet",
                    style = MaterialTheme.typography.titleMedium
                )
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
                                    editState = EditState(conn.id, conn.name, conn.baseUrl, conn.token)
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

    // ── Edit / Add dialog ──────────────────────────────────────────────────────
    editState?.let { state ->
        ConnectionEditDialog(
            isNew = state.id == null,
            initial = state,
            addressResult = addressResult,
            apiResult = apiResult,
            isTesting = isTesting,
            onTest = { url, token -> viewModel.runConnectionTest(url, token) },
            onSave = { name, url, token ->
                if (state.id == null) {
                    viewModel.addConnection(name, url, token)
                } else {
                    viewModel.updateConnection(state.id, name, url, token)
                }
                editState = null
            },
            onDismiss = { editState = null }
        )
    }

    // ── Delete confirmation ────────────────────────────────────────────────────
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

@Composable
private fun ConnectionEditDialog(
    isNew: Boolean,
    initial: EditState,
    addressResult: AddressProbeResult?,
    apiResult: ApiProbeResult?,
    isTesting: Boolean,
    onTest: (String, String) -> Unit,
    onSave: (name: String, url: String, token: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var url by remember { mutableStateOf(initial.baseUrl) }
    var token by remember { mutableStateOf(initial.token) }
    var showHelp by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add connection" else "Edit connection") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("My Home") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Base URL") },
                    placeholder = { Text("https://your-ha.duckdns.org") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Long-Lived Access Token") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ConnectionResultRow(
                            icon = Icons.Rounded.Link,
                            label = "Server",
                            result = addressResult,
                            isLoading = isTesting
                        )
                        ConnectionResultRow(
                            icon = Icons.Rounded.VpnKey,
                            label = "API",
                            result = apiResult,
                            isLoading = isTesting
                        )
                        OutlinedButton(
                            onClick = { onTest(url, token) },
                            enabled = !isTesting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (isTesting) "Testing…" else "Test connection")
                        }
                    }
                }

                // ── Help section ──────────────────────────────────────────
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Help,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "How to connect",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showHelp = !showHelp },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    if (showHelp) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = if (showHelp) "Collapse" else "Expand",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (showHelp) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                "Getting your access token:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "1. Open Home Assistant in a browser\n" +
                                "2. Click your profile picture (bottom-left sidebar)\n" +
                                "3. Scroll down to \"Long-Lived Access Tokens\"\n" +
                                "4. Click \"Create Token\", give it a name\n" +
                                "5. Copy the token and paste it above",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "WebSocket API is enabled by default in all modern Home Assistant installations. No extra configuration is needed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, url, token) },
                enabled = url.isNotBlank() && token.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ConnectionResultRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    result: Any?,
    isLoading: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(48.dp))
        when {
            isLoading && result == null -> {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Text("Checking…", style = MaterialTheme.typography.bodySmall)
            }
            result is AddressProbeResult.Reachable ->
                StatusText(true, "HTTP ${result.httpCode}")
            result is AddressProbeResult.Unreachable ->
                StatusText(false, result.detail.take(60))
            result is AddressProbeResult.InvalidInput ->
                StatusText(false, result.detail)
            result is ApiProbeResult.Ok ->
                StatusText(true, "OK")
            result is ApiProbeResult.HttpError ->
                StatusText(false, "HTTP ${result.code}")
            result is ApiProbeResult.NetworkError ->
                StatusText(false, result.detail.take(60))
            result is ApiProbeResult.Skipped ->
                Text(result.reason, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            else ->
                Text("—", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusText(ok: Boolean, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (ok) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.error
        )
    }
}
