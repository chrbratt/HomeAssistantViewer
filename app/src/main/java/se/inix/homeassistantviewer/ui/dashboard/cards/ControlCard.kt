package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
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
import se.inix.homeassistantviewer.domain.entity.brightnessPercent
import se.inix.homeassistantviewer.domain.entity.fanPercentage
import se.inix.homeassistantviewer.domain.entity.supportsBrightness
import se.inix.homeassistantviewer.ui.common.getIconForEntity
import se.inix.homeassistantviewer.ui.dashboard.DashboardItem
import se.inix.homeassistantviewer.ui.dashboard.EntityAction

/**
 * Toggleable on/off control with optional dimming or fan-speed slider.
 * Handles `light`, `switch`, `input_boolean` and `fan` domains.
 */
@Composable
internal fun ControlCard(
    item: DashboardItem.Entity,
    onAction: (EntityAction) -> Unit,
    modifier: Modifier = Modifier,
    onOpenDetail: (() -> Unit)? = null
) {
    val entity = item.entity ?: return
    val isOn = entity.state == "on"
    val isFan = entity.domain == "fan"
    val colors = rememberCardColors(active = isOn, animationLabel = "controlCardBg")

    var isDragging by remember { mutableStateOf(false) }

    var localBrightness by remember(entity.entityId) {
        mutableFloatStateOf((entity.brightnessPercent ?: 100).toFloat())
    }
    val remoteBrightness = entity.brightnessPercent?.toFloat()
    // LaunchedEffect lets remote updates win when the user isn't dragging.
    LaunchedEffect(remoteBrightness, isDragging) {
        if (!isDragging && remoteBrightness != null) localBrightness = remoteBrightness
    }

    var localFanPct by remember(entity.entityId) {
        mutableFloatStateOf((entity.fanPercentage ?: 50).toFloat())
    }
    val remoteFanPct = entity.fanPercentage?.toFloat()
    LaunchedEffect(remoteFanPct, isDragging) {
        if (!isDragging && remoteFanPct != null) localFanPct = remoteFanPct
    }

    DashboardCardShell(
        title = cardDisplayTitle(item),
        colors = colors,
        modifier = modifier,
        onClick = { onAction(EntityAction.Toggle(item.connectionId, item.entityId)) },
        onOpenDetail = onOpenDetail
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CardStyle.Spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardIconBadge(
                icon = getIconForEntity(entity),
                tint = colors.onContainer,
                backgroundColor = if (isOn) colors.onContainer.copy(alpha = 0.12f)
                                  else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            )
            if (isOn && entity.supportsBrightness) {
                Text(
                    text = "${localBrightness.toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onContainer.copy(alpha = 0.7f)
                )
            }
            if (isOn && isFan && entity.fanPercentage != null) {
                Text(
                    text = "${localFanPct.toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onContainer.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            CardSwitch(
                checked = isOn,
                onCheckedChange = {
                    onAction(EntityAction.Toggle(item.connectionId, item.entityId))
                }
            )
        }

        if (isOn && entity.supportsBrightness) {
            CardSlider(
                value = localBrightness,
                onValueChange = { isDragging = true; localBrightness = it },
                onValueChangeFinished = {
                    isDragging = false
                    // 0 % is interpreted as "turn off" in the dispatcher so
                    // the light cannot end up in an ambiguous "on at brightness=0" state.
                    onAction(EntityAction.SetBrightness(item.connectionId, item.entityId, localBrightness.toInt()))
                },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (isOn && isFan && entity.fanPercentage != null) {
            CardSlider(
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
