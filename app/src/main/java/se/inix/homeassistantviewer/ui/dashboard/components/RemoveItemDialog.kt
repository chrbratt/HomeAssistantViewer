package se.inix.homeassistantviewer.ui.dashboard.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import se.inix.homeassistantviewer.ui.dashboard.DashboardItem
import se.inix.homeassistantviewer.ui.dashboard.cards.cardDisplayTitle

/** Confirmation dialog before removing an entity card or a row break. */
@Composable
fun RemoveItemDialog(
    item: DashboardItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, message) = when (item) {
        is DashboardItem.Entity -> {
            // Use the same label the card itself shows (customName → friendly
            // → entityId) so the confirmation matches what the user sees.
            val label = cardDisplayTitle(item)
            "Remove from dashboard?" to
                "\"$label\" will no longer appear on the dashboard. You can add it again later from the entity picker."
        }
        is DashboardItem.Divider -> {
            val suffix = item.title?.takeUnless { it.isBlank() }
                ?.let { " \"$it\"" }.orEmpty()
            "Remove section break$suffix?" to
                "The cards above and below will flow together again."
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { Button(onClick = onConfirm) { Text("Remove") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
