package se.inix.homeassistantviewer.ui.dashboard.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import se.inix.homeassistantviewer.ui.dashboard.DashboardItem

/** Confirmation dialog before removing an entity card or a row break. */
@Composable
fun RemoveItemDialog(
    item: DashboardItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, message) = when (item) {
        is DashboardItem.Entity -> {
            val label = item.entity?.friendlyName ?: item.entityId
            "Remove from dashboard?" to
                "\"$label\" will no longer appear on the dashboard. You can add it again later from the entity picker."
        }
        is DashboardItem.Divider ->
            "Remove row break?" to "The cards above and below will flow together again."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { Button(onClick = onConfirm) { Text("Remove") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
