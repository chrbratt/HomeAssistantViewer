package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.ui.dashboard.DashboardItem
import se.inix.homeassistantviewer.ui.dashboard.EntityAction

/**
 * Single-tap "Run" card for entities that don't have a persistent state worth
 * toggling — scripts, scenes and automations.
 */
@Composable
internal fun ActivateCard(
    item: DashboardItem.Entity,
    onAction: (EntityAction) -> Unit,
    modifier: Modifier = Modifier,
    onRequestRename: (() -> Unit)? = null
) {
    val entity = item.entity ?: return
    val domain = entity.domain
    val label = when (domain) {
        "script" -> "Run"
        "automation" -> "Trigger"
        else -> "Activate"
    }
    val action: EntityAction = when (domain) {
        "automation" -> EntityAction.TriggerAutomation(item.connectionId, item.entityId)
        else -> EntityAction.Activate(item.connectionId, item.entityId)
    }
    val colors = rememberCardColors(active = false, animationLabel = "activateCardBg")

    DashboardCardShell(
        title = cardDisplayTitle(item),
        colors = colors,
        modifier = modifier,
        onClick = { onAction(action) },
        onRequestRename = onRequestRename
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardIconBadge(
                icon = Icons.Rounded.PlayCircle,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = { onAction(action) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(text = label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
