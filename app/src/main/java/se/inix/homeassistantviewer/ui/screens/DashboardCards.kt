package se.inix.homeassistantviewer.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Blinds
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Toys
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.data.model.HaEntityState
import se.inix.homeassistantviewer.viewmodel.DashboardEntity
import se.inix.homeassistantviewer.viewmodel.EntityAction

// ── Entity card dispatcher ─────────────────────────────────────────────────────

@Composable
fun EntityCard(
    item: DashboardEntity,
    onAction: (EntityAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val entity = item.entity
    if (entity == null) {
        UnavailableEntityCard(entityId = item.entityId, modifier = modifier)
        return
    }

    when (entity.domain) {
        "light", "switch", "input_boolean", "automation", "fan" ->
            ControlCard(item = item, onAction = onAction, modifier = modifier)
        "cover" ->
            CoverCard(item = item, onAction = onAction, modifier = modifier)
        "climate" ->
            ClimateCard(item = item, onAction = onAction, modifier = modifier)
        "lock" ->
            LockCard(item = item, onAction = onAction, modifier = modifier)
        "media_player" ->
            MediaPlayerCard(item = item, onAction = onAction, modifier = modifier)
        "input_number" ->
            InputNumberCard(item = item, onAction = onAction, modifier = modifier)
        "scene", "script" ->
            ActivateCard(item = item, onAction = onAction, modifier = modifier)
        else ->
            SensorCard(entity = entity, modifier = modifier)
    }
}

// ── Control card (light / switch / input_boolean / automation / fan) ───────────

@Composable
internal fun ControlCard(
    item: DashboardEntity,
    onAction: (EntityAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val entity = item.entity ?: return
    val isOn = entity.state == "on"
    val isFan = entity.domain == "fan"

    val containerColor by animateColorAsState(
        targetValue = if (isOn) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = spring(),
        label = "controlCardBg"
    )
    val onContainerColor = if (isOn) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant

    var isDragging by remember { mutableStateOf(false) }

    // Brightness (light)
    var localBrightness by remember(entity.entityId) {
        mutableFloatStateOf((entity.brightnessPercent ?: 100).toFloat())
    }
    val remoteBrightness = entity.brightnessPercent?.toFloat()
    if (!isDragging && remoteBrightness != null) localBrightness = remoteBrightness

    // Fan percentage
    var localFanPct by remember(entity.entityId) {
        mutableFloatStateOf((entity.fanPercentage ?: 50).toFloat())
    }
    val remoteFanPct = entity.fanPercentage?.toFloat()
    if (!isDragging && remoteFanPct != null) localFanPct = remoteFanPct

    Card(
        onClick = { onAction(EntityAction.Toggle(item.connectionId, item.entityId)) },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isOn) onContainerColor.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconForEntity(entity),
                        contentDescription = null,
                        tint = onContainerColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Switch(checked = isOn, onCheckedChange = {
                    onAction(EntityAction.Toggle(item.connectionId, item.entityId))
                })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = entity.friendlyName ?: entity.entityId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onContainerColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isOn && entity.supportsBrightness) {
                    Text(
                        text = "${localBrightness.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = onContainerColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                    )
                }
                if (isOn && isFan && entity.fanPercentage != null) {
                    Text(
                        text = "${localFanPct.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = onContainerColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                    )
                }
            }

            Text(
                text = if (isOn) "On" else "Off",
                style = MaterialTheme.typography.bodySmall,
                color = onContainerColor.copy(alpha = 0.6f)
            )

            if (isOn && entity.supportsBrightness) {
                Slider(
                    value = localBrightness,
                    onValueChange = { isDragging = true; localBrightness = it },
                    onValueChangeFinished = {
                        isDragging = false
                        onAction(EntityAction.SetBrightness(item.connectionId, item.entityId, localBrightness.toInt()))
                    },
                    valueRange = 1f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isOn && isFan && entity.fanPercentage != null) {
                Slider(
                    value = localFanPct,
                    onValueChange = { isDragging = true; localFanPct = it },
                    onValueChangeFinished = {
                        isDragging = false
                        onAction(EntityAction.SetFanPercentage(item.connectionId, item.entityId, localFanPct.toInt()))
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ── Cover card ─────────────────────────────────────────────────────────────────

@Composable
internal fun CoverCard(
    item: DashboardEntity,
    onAction: (EntityAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val entity = item.entity ?: return
    val position = entity.coverPosition
    val isOpen = entity.state == "open" || entity.state == "opening"

    val containerColor by animateColorAsState(
        targetValue = if (isOpen) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = spring(),
        label = "coverCardBg"
    )
    val onContainerColor = if (isOpen) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant

    var isDragging by remember { mutableStateOf(false) }
    var localPosition by remember(entity.entityId) {
        mutableFloatStateOf((position ?: if (isOpen) 100 else 0).toFloat())
    }
    val remotePosition = position?.toFloat()
    if (!isDragging && remotePosition != null) localPosition = remotePosition

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            onContainerColor.copy(alpha = 0.12f), CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Blinds, contentDescription = null,
                        tint = onContainerColor, modifier = Modifier.size(22.dp)
                    )
                }
                // Open / Stop / Close buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { onAction(EntityAction.OpenCover(item.connectionId, item.entityId)) }) {
                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Open",
                            tint = onContainerColor)
                    }
                    IconButton(onClick = { onAction(EntityAction.StopCover(item.connectionId, item.entityId)) }) {
                        Icon(Icons.Rounded.Stop, contentDescription = "Stop",
                            tint = onContainerColor)
                    }
                    IconButton(onClick = { onAction(EntityAction.CloseCover(item.connectionId, item.entityId)) }) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close",
                            tint = onContainerColor)
                    }
                }
            }

            Text(
                text = entity.friendlyName ?: entity.entityId,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = onContainerColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = entity.state.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = onContainerColor.copy(alpha = 0.6f)
            )

            if (position != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Slider(
                        value = localPosition,
                        onValueChange = { isDragging = true; localPosition = it },
                        onValueChangeFinished = {
                            isDragging = false
                            onAction(EntityAction.SetCoverPosition(item.connectionId, item.entityId, localPosition.toInt()))
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${localPosition.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = onContainerColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ── Climate card ───────────────────────────────────────────────────────────────

@Composable
internal fun ClimateCard(
    item: DashboardEntity,
    onAction: (EntityAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val entity = item.entity ?: return
    val currentTemp = entity.currentTemperature
    val isOff = entity.state == "off"

    val containerColor by animateColorAsState(
        targetValue = if (!isOff) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = spring(),
        label = "climateCardBg"
    )
    val onContainerColor = if (!isOff) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant

    var localTarget by remember(entity.entityId) {
        mutableFloatStateOf((entity.targetTemperature ?: 21.0).toFloat())
    }
    val remoteTarget = entity.targetTemperature?.toFloat()
    if (remoteTarget != null) localTarget = remoteTarget

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(onContainerColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Thermostat, contentDescription = null,
                        tint = onContainerColor, modifier = Modifier.size(22.dp)
                    )
                }
                if (currentTemp != null) {
                    Text(
                        text = "%.1f°".format(currentTemp),
                        style = MaterialTheme.typography.titleMedium,
                        color = onContainerColor
                    )
                }
            }

            Text(
                text = entity.friendlyName ?: entity.entityId,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = onContainerColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Target temperature row
            if (!isOff) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Target:",
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainerColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            val newTemp = (localTarget - 0.5f).coerceIn(5f, 35f)
                            localTarget = newTemp
                            onAction(EntityAction.SetClimateTemperature(item.connectionId, item.entityId, newTemp.toDouble()))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Decrease",
                            tint = onContainerColor, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "%.1f°".format(localTarget),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onContainerColor
                    )
                    IconButton(
                        onClick = {
                            val newTemp = (localTarget + 0.5f).coerceIn(5f, 35f)
                            localTarget = newTemp
                            onAction(EntityAction.SetClimateTemperature(item.connectionId, item.entityId, newTemp.toDouble()))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Increase",
                            tint = onContainerColor, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // HVAC mode chips
            if (entity.hvacModes.isNotEmpty()) {
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entity.hvacModes.forEach { mode ->
                        FilterChip(
                            selected = entity.state == mode,
                            onClick = {
                                onAction(EntityAction.SetClimateHvacMode(item.connectionId, item.entityId, mode))
                            },
                            label = {
                                Text(
                                    text = mode.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Lock card ──────────────────────────────────────────────────────────────────

@Composable
internal fun LockCard(
    item: DashboardEntity,
    onAction: (EntityAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val entity = item.entity ?: return
    val isLocked = entity.state == "locked"

    val containerColor by animateColorAsState(
        targetValue = if (isLocked) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.errorContainer,
        animationSpec = spring(),
        label = "lockCardBg"
    )
    val onContainerColor = if (isLocked) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onErrorContainer

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(onContainerColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        contentDescription = null,
                        tint = onContainerColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Button(
                    onClick = {
                        if (isLocked) onAction(EntityAction.Unlock(item.connectionId, item.entityId))
                        else onAction(EntityAction.Lock(item.connectionId, item.entityId))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = onContainerColor.copy(alpha = 0.15f),
                        contentColor = onContainerColor
                    )
                ) {
                    Text(text = if (isLocked) "Unlock" else "Lock")
                }
            }

            Text(
                text = entity.friendlyName ?: entity.entityId,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = onContainerColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isLocked) "Locked" else "Unlocked",
                style = MaterialTheme.typography.bodySmall,
                color = onContainerColor.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Media player card ──────────────────────────────────────────────────────────

@Composable
internal fun MediaPlayerCard(
    item: DashboardEntity,
    onAction: (EntityAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val entity = item.entity ?: return
    val isPlaying = entity.state == "playing"
    val isActive = entity.state != "off" && entity.state != "unavailable"

    val containerColor by animateColorAsState(
        targetValue = if (isPlaying) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = spring(),
        label = "mediaCardBg"
    )
    val onContainerColor = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant

    var isDragging by remember { mutableStateOf(false) }
    var localVolume by remember(entity.entityId) {
        mutableFloatStateOf(entity.volumeLevel ?: 0.5f)
    }
    val remoteVolume = entity.volumeLevel
    if (!isDragging && remoteVolume != null) localVolume = remoteVolume

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(onContainerColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow, contentDescription = null,
                        tint = onContainerColor, modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = entity.state.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = onContainerColor.copy(alpha = 0.7f)
                )
            }

            Text(
                text = entity.friendlyName ?: entity.entityId,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = onContainerColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (entity.mediaTitle != null || entity.mediaArtist != null) {
                Text(
                    text = listOfNotNull(entity.mediaTitle, entity.mediaArtist).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainerColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Playback controls
            if (isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onAction(EntityAction.MediaPrevious(item.connectionId, item.entityId)) }) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous",
                            tint = onContainerColor, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onAction(EntityAction.MediaPlayPause(item.connectionId, item.entityId)) }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = onContainerColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onAction(EntityAction.MediaNext(item.connectionId, item.entityId)) }) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = "Next",
                            tint = onContainerColor, modifier = Modifier.size(28.dp))
                    }
                }

                // Volume
                if (entity.volumeLevel != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null,
                            tint = onContainerColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp))
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
}

// ── Input number card ──────────────────────────────────────────────────────────

@Composable
internal fun InputNumberCard(
    item: DashboardEntity,
    onAction: (EntityAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val entity = item.entity ?: return
    val currentValue = entity.inputNumberValue ?: return

    var isDragging by remember { mutableStateOf(false) }
    var localValue by remember(entity.entityId) { mutableFloatStateOf(currentValue.toFloat()) }
    if (!isDragging) localValue = currentValue.toFloat()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Sensors, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = formatSensorValue(localValue.toString()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                text = entity.friendlyName ?: entity.entityId,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Slider(
                value = localValue,
                onValueChange = { isDragging = true; localValue = it },
                onValueChangeFinished = {
                    isDragging = false
                    onAction(EntityAction.SetInputNumber(item.connectionId, item.entityId, localValue.toDouble()))
                },
                valueRange = entity.inputNumberMin.toFloat()..entity.inputNumberMax.toFloat(),
                steps = if (entity.inputNumberStep > 0) {
                    ((entity.inputNumberMax - entity.inputNumberMin) / entity.inputNumberStep).toInt() - 1
                } else 0,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatSensorValue(entity.inputNumberMin.toString()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
                Text(
                    text = formatSensorValue(entity.inputNumberMax.toString()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ── Activate card (scene / script) ─────────────────────────────────────────────

@Composable
internal fun ActivateCard(
    item: DashboardEntity,
    onAction: (EntityAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val entity = item.entity ?: return
    val isScript = entity.domain == "script"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlayCircle, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = entity.friendlyName ?: entity.entityId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(
                onClick = { onAction(EntityAction.Activate(item.connectionId, item.entityId)) },
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Text(text = if (isScript) "Run" else "Activate")
            }
        }
    }
}

// ── Sensor card (read-only) ────────────────────────────────────────────────────

@Composable
internal fun SensorCard(entity: HaEntityState, modifier: Modifier = Modifier) {
    val icon = getIconForEntity(entity)
    val iconTint = sensorIconTint(entity)
    val unit = entity.unitOfMeasurement
    val isBinary = entity.domain == "binary_sensor" && unit == null &&
        (entity.state == "on" || entity.state == "off")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = entity.friendlyName ?: entity.entityId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconTint.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon, contentDescription = null,
                        tint = iconTint, modifier = Modifier.size(20.dp)
                    )
                }
                when {
                    isBinary -> BinaryStateIndicator(isActive = entity.state == "on")
                    unit != null -> NumericValueDisplay(value = entity.state, unit = unit)
                    else -> Text(
                        text = formatSensorValue(entity.state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ── Unavailable card ───────────────────────────────────────────────────────────

@Composable
internal fun UnavailableEntityCard(entityId: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.CloudOff, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entityId.substringAfterLast(".").replace("_", " ")
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Unavailable",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ── Shared value widgets ───────────────────────────────────────────────────────

@Composable
internal fun NumericValueDisplay(value: String, unit: String) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = formatSensorValue(value),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 3.dp)
        )
    }
}

@Composable
internal fun BinaryStateIndicator(isActive: Boolean) {
    val dotColor = if (isActive) Color(0xFF4CAF50)
                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val label = if (isActive) "Active" else "Clear"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Value formatting ───────────────────────────────────────────────────────────

internal fun formatSensorValue(state: String): String {
    val d = state.toDoubleOrNull() ?: return state.replaceFirstChar { it.uppercase() }
    return if (d % 1.0 == 0.0) d.toLong().toString() else "%.1f".format(d)
}

// ── Icon resolution ────────────────────────────────────────────────────────────

fun getIconForEntity(entity: HaEntityState): ImageVector {
    val dc = entity.attributes?.get("device_class") as? String
    return when {
        entity.domain == "light" -> Icons.Rounded.Lightbulb
        entity.domain == "switch" -> Icons.Rounded.Power
        entity.domain == "climate" -> Icons.Rounded.Thermostat
        entity.domain == "cover" -> Icons.Rounded.Blinds
        entity.domain == "fan" -> Icons.Rounded.Toys
        entity.domain == "lock" -> Icons.Rounded.Lock
        entity.domain == "media_player" -> Icons.Rounded.PlayArrow
        entity.domain == "weather" -> Icons.Rounded.WbCloudy
        dc == "temperature" -> Icons.Rounded.Thermostat
        dc == "humidity" || dc == "moisture" -> Icons.Rounded.WaterDrop
        dc == "wind_speed" || dc == "wind_direction" -> Icons.Rounded.Air
        dc == "illuminance" -> Icons.Rounded.WbSunny
        dc == "battery" -> Icons.Rounded.BatteryFull
        dc == "power" || dc == "energy" || dc == "voltage" || dc == "current" -> Icons.Rounded.Bolt
        dc == "motion" || dc == "occupancy" || dc == "presence" -> Icons.AutoMirrored.Rounded.DirectionsWalk
        else -> Icons.Rounded.Sensors
    }
}

fun getIconForDomain(domain: String): ImageVector = when (domain) {
    "light" -> Icons.Rounded.Lightbulb
    "switch" -> Icons.Rounded.Power
    "climate" -> Icons.Rounded.Thermostat
    "cover" -> Icons.Rounded.Blinds
    "fan" -> Icons.Rounded.Toys
    "lock" -> Icons.Rounded.Lock
    "media_player" -> Icons.Rounded.PlayArrow
    "weather" -> Icons.Rounded.WbCloudy
    "sensor" -> Icons.Rounded.Sensors
    "binary_sensor" -> Icons.Rounded.Sensors
    else -> Icons.Rounded.Sensors
}

// ── Temperature-aware icon tinting ─────────────────────────────────────────────

@Composable
internal fun sensorIconTint(entity: HaEntityState): Color {
    val dc = entity.attributes?.get("device_class") as? String
    val numericValue = entity.state.toDoubleOrNull()

    if (dc == "temperature" && numericValue != null) {
        return when {
            numericValue < 0 -> Color(0xFF90CAF9)
            numericValue < 8 -> Color(0xFF80DEEA)
            numericValue < 18 -> Color(0xFFA5D6A7)
            numericValue < 26 -> Color(0xFFFFCC80)
            else -> Color(0xFFEF9A9A)
        }
    }

    return when (dc) {
        "humidity", "moisture" -> Color(0xFF81D4FA)
        "wind_speed", "wind_direction" -> Color(0xFF80CBC4)
        "illuminance" -> Color(0xFFFFF176)
        "battery" -> Color(0xFFA5D6A7)
        "power", "energy", "voltage", "current" -> Color(0xFFFFCC80)
        "motion", "occupancy", "presence" ->
            if (entity.state == "on") Color(0xFFFFB74D) else Color(0xFF9E9E9E)
        else -> MaterialTheme.colorScheme.primary
    }
}
