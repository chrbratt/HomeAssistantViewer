package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Blinds
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.domain.entity.coverPosition
import se.inix.homeassistantviewer.ui.dashboard.DashboardItem
import se.inix.homeassistantviewer.ui.dashboard.EntityAction

@Composable
internal fun CoverCard(
    item: DashboardItem.Entity,
    onAction: (EntityAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val entity = item.entity ?: return
    val position = entity.coverPosition
    val isOpen = entity.state == "open" || entity.state == "opening"
    val colors = rememberCardColors(active = isOpen, animationLabel = "coverCardBg")

    var isDragging by remember { mutableStateOf(false) }
    var localPosition by remember(entity.entityId) {
        mutableFloatStateOf((position ?: if (isOpen) 100 else 0).toFloat())
    }
    val remotePosition = position?.toFloat()
    LaunchedEffect(remotePosition, isDragging) {
        if (!isDragging && remotePosition != null) localPosition = remotePosition
    }

    DashboardCardShell(
        title = entity.friendlyName ?: entity.entityId,
        colors = colors,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardIconBadge(icon = Icons.Rounded.Blinds, tint = colors.onContainer)
            IconButton(
                onClick = { onAction(EntityAction.OpenCover(item.connectionId, item.entityId)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Open", tint = colors.onContainer,
                    modifier = Modifier.size(20.dp))
            }
            IconButton(
                onClick = { onAction(EntityAction.StopCover(item.connectionId, item.entityId)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Rounded.Stop, contentDescription = "Stop", tint = colors.onContainer,
                    modifier = Modifier.size(20.dp))
            }
            IconButton(
                onClick = { onAction(EntityAction.CloseCover(item.connectionId, item.entityId)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close", tint = colors.onContainer,
                    modifier = Modifier.size(20.dp))
            }
            if (position != null) {
                Slider(
                    value = localPosition,
                    onValueChange = { isDragging = true; localPosition = it },
                    onValueChangeFinished = {
                        isDragging = false
                        onAction(EntityAction.SetCoverPosition(item.connectionId, item.entityId, localPosition.toInt()))
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )
                Text(
                    text = "${localPosition.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
