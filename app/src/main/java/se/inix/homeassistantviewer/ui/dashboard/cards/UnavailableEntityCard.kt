package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Shown when a favourited entity is no longer available on the server (renamed,
 * removed, or the connection is offline). The explicit close button is needed
 * because long-press is reserved for drag-to-reorder — without it an orphan
 * favourite would be impossible to remove.
 *
 * This card intentionally bypasses [DashboardCardShell]: the layout is a
 * single horizontal row (no separate title line) and the visual treatment is
 * deliberately faded to communicate "not interactive".
 */
@Composable
internal fun UnavailableEntityCard(
    entityId: String,
    onRequestRemove: () -> Unit,
    modifier: Modifier = Modifier,
    customName: String? = null
) {
    // Honour a user-set name even when the entity itself can't be loaded —
    // it's the name the user knows it by. Falls back to a humanised
    // entity-id form ("light.living_room" → "Living room") as before.
    val displayName = customName?.takeUnless { it.isBlank() }
        ?: entityId.substringAfterLast(".")
            .replace("_", " ")
            .replaceFirstChar { it.uppercase() }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardStyle.Padding),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardIconBadge(
                icon = Icons.Rounded.CloudOff,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Unavailable",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            IconButton(onClick = onRequestRemove, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Remove from dashboard",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
