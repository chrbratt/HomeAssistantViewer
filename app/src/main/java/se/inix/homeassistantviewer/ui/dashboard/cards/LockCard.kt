package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.ui.dashboard.DashboardItem
import se.inix.homeassistantviewer.ui.dashboard.EntityAction

@Composable
internal fun LockCard(
    item: DashboardItem.Entity,
    onAction: (EntityAction) -> Unit,
    modifier: Modifier = Modifier,
    onOpenDetail: (() -> Unit)? = null
) {
    val entity = item.entity ?: return
    val isLocked = entity.state == "locked"
    // Locks have a non-standard "alarm" inactive state — show errorContainer when
    // unlocked so the user instantly sees something is off.
    val colors = rememberCardColors(
        active = isLocked,
        inactiveContainer = MaterialTheme.colorScheme.errorContainer,
        inactiveOnContainer = MaterialTheme.colorScheme.onErrorContainer,
        animationLabel = "lockCardBg"
    )

    val toggle = {
        if (isLocked) onAction(EntityAction.Unlock(item.connectionId, item.entityId))
        else onAction(EntityAction.Lock(item.connectionId, item.entityId))
    }

    DashboardCardShell(
        title = entity.friendlyName ?: entity.entityId,
        colors = colors,
        modifier = modifier,
        onClick = toggle,
        onOpenDetail = onOpenDetail
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardIconBadge(
                icon = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                tint = colors.onContainer
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = toggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.onContainer.copy(alpha = 0.15f),
                    contentColor = colors.onContainer
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(text = if (isLocked) "Unlock" else "Lock",
                    style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
