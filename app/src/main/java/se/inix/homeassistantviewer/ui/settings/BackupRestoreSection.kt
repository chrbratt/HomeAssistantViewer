package se.inix.homeassistantviewer.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.inix.homeassistantviewer.data.backup.BACKUP_MIME_TYPE
import se.inix.homeassistantviewer.data.backup.InternalSnapshotStore
import se.inix.homeassistantviewer.ui.common.EditTextDialog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BackupRestoreSection(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snapshots by viewModel.internalSnapshots.collectAsStateWithLifecycle()

    var savingSnapshot by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var restoreConfirmId by remember { mutableStateOf<String?>(null) }
    var deleteConfirmId by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)
    ) { uri ->
        if (uri != null) {
            viewModel.exportToUri(context.contentResolver, uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
        }
    }

    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Backup & restore", style = MaterialTheme.typography.titleMedium)
            Text(
                "Export or import connections, API tokens, favourites, custom names, " +
                    "row breaks and dashboard layout. Internal snapshots let you switch " +
                    "layouts quickly on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Backup files contain API tokens in plain text. Store them securely.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { exportLauncher.launch(viewModel.suggestExportFileName()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                    Text("Export", modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf(BACKUP_MIME_TYPE, "application/*")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Upload, contentDescription = null)
                    Text("Import", modifier = Modifier.padding(start = 6.dp))
                }
            }

            Text("On-device snapshots", style = MaterialTheme.typography.titleSmall)

            OutlinedButton(
                onClick = { savingSnapshot = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null)
                Text("Save current layout as snapshot", modifier = Modifier.padding(start = 6.dp))
            }

            if (snapshots.isEmpty()) {
                Text(
                    "No snapshots yet. Save one before trying a new layout.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                snapshots.forEach { meta ->
                    SnapshotRow(
                        meta = meta,
                        onRestore = { restoreConfirmId = meta.id },
                        onDelete = { deleteConfirmId = meta.id }
                    )
                }
            }
        }
    }

    if (savingSnapshot) {
        EditTextDialog(
            title = "Save snapshot",
            label = "Snapshot name",
            initialValue = "",
            placeholder = "e.g. Production layout",
            supportingText = "Up to ${InternalSnapshotStore.MAX_SNAPSHOTS} snapshots are kept on this device.",
            onConfirm = { name ->
                viewModel.saveInternalSnapshot(name ?: "Snapshot")
                savingSnapshot = false
            },
            onDismiss = { savingSnapshot = false }
        )
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Import backup?") },
            text = {
                Text(
                    "This replaces all connections, favourites and dashboard settings " +
                        "on this device. This cannot be undone unless you have a snapshot " +
                        "or export file saved."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importFromUri(context.contentResolver, uri)
                        pendingImportUri = null
                    }
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("Cancel") }
            }
        )
    }

    restoreConfirmId?.let { id ->
        val name = snapshots.firstOrNull { it.id == id }?.name ?: "snapshot"
        AlertDialog(
            onDismissRequest = { restoreConfirmId = null },
            title = { Text("Restore snapshot?") },
            text = {
                Text(
                    "Restore \"$name\"? Current connections and favourites will be replaced."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreInternalSnapshot(id)
                        restoreConfirmId = null
                    }
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { restoreConfirmId = null }) { Text("Cancel") }
            }
        )
    }

    deleteConfirmId?.let { id ->
        val name = snapshots.firstOrNull { it.id == id }?.name ?: "snapshot"
        AlertDialog(
            onDismissRequest = { deleteConfirmId = null },
            title = { Text("Delete snapshot?") },
            text = { Text("Delete \"$name\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteInternalSnapshot(id)
                        deleteConfirmId = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SnapshotRow(
    meta: InternalSnapshotStore.SnapshotMeta,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val savedLabel = remember(meta.savedAtEpoch) {
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
            .withZone(ZoneId.systemDefault())
        formatter.format(Instant.ofEpochSecond(meta.savedAtEpoch))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(meta.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                savedLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRestore) {
            Icon(Icons.Rounded.Restore, contentDescription = "Restore snapshot")
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = "Delete snapshot",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
