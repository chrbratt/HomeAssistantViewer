package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.domain.entity.mediaArtist
import se.inix.homeassistantviewer.domain.entity.mediaTitle
import se.inix.homeassistantviewer.domain.entity.volumeLevel
import se.inix.homeassistantviewer.ui.dashboard.DashboardItem
import se.inix.homeassistantviewer.ui.dashboard.EntityAction

@Composable
internal fun MediaPlayerCard(
    item: DashboardItem.Entity,
    onAction: (EntityAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val entity = item.entity ?: return
    val isPlaying = entity.state == "playing"
    val isActive = entity.state != "off" && entity.state != "unavailable"
    val colors = rememberCardColors(active = isPlaying, animationLabel = "mediaCardBg")

    var isDragging by remember { mutableStateOf(false) }
    var localVolume by remember(entity.entityId) {
        mutableFloatStateOf(entity.volumeLevel ?: 0.5f)
    }
    val remoteVolume = entity.volumeLevel
    LaunchedEffect(remoteVolume, isDragging) {
        if (!isDragging && remoteVolume != null) localVolume = remoteVolume
    }

    DashboardCardShell(
        title = entity.friendlyName ?: entity.entityId,
        colors = colors,
        modifier = modifier
    ) {
        if (entity.mediaTitle != null || entity.mediaArtist != null) {
            Text(
                text = listOfNotNull(entity.mediaTitle, entity.mediaArtist).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onContainer.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onAction(EntityAction.MediaPrevious(item.connectionId, item.entityId)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous",
                        tint = colors.onContainer, modifier = Modifier.size(22.dp))
                }
                IconButton(
                    onClick = { onAction(EntityAction.MediaPlayPause(item.connectionId, item.entityId)) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = colors.onContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(
                    onClick = { onAction(EntityAction.MediaNext(item.connectionId, item.entityId)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next",
                        tint = colors.onContainer, modifier = Modifier.size(22.dp))
                }
                if (entity.volumeLevel != null) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null,
                        tint = colors.onContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(16.dp))
                    Slider(
                        value = localVolume,
                        onValueChange = { isDragging = true; localVolume = it },
                        onValueChangeFinished = {
                            isDragging = false
                            onAction(EntityAction.SetMediaVolume(item.connectionId, item.entityId, localVolume))
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
