package se.inix.homeassistantviewer.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.data.settings.CardTimestamp

/**
 * Combined "rename + per-card settings" dialog for the entity detail screen.
 *
 * Beyond the display name it exposes the per-entity card-timestamp override.
 * "Default" stores `null` (inherit the global setting); any explicit choice
 * wins over the global default — that's the contract the dashboard resolves
 * via `resolveCardTimestamp`.
 */
@Composable
internal fun EntityDetailEditDialog(
    initialName: String,
    haName: String,
    initialOverride: CardTimestamp?,
    onConfirm: (name: String?, override: CardTimestamp?) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialName) }
    var override by remember { mutableStateOf(initialOverride) }

    // null = "Default" (inherit global); the rest map 1:1 to CardTimestamp.
    val options: List<Pair<CardTimestamp?, String>> = listOf(
        null to "Default",
        CardTimestamp.NONE to "Off",
        CardTimestamp.LAST_UPDATED to "Updated",
        CardTimestamp.LAST_REPORTED to "Reported",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Entity settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Display name") },
                        placeholder = { Text(haName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Leave empty to use the Home Assistant name (\"$haName\").",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Card timestamp", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "\"Default\" follows the global setting. Any other choice " +
                            "overrides it for just this entity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        options.forEachIndexed { index, (option, label) ->
                            SegmentedButton(
                                selected = override == option,
                                onClick = { override = option },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index, count = options.size
                                ),
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(text.trim().takeIf { it.isNotEmpty() }, override)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
