package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.domain.entity.currentTemperature
import se.inix.homeassistantviewer.domain.entity.hvacModes
import se.inix.homeassistantviewer.domain.entity.targetTemperature
import se.inix.homeassistantviewer.ui.dashboard.DashboardItem
import se.inix.homeassistantviewer.ui.dashboard.EntityAction

@Composable
internal fun ClimateCard(
    item: DashboardItem.Entity,
    onAction: (EntityAction) -> Unit,
    modifier: Modifier = Modifier,
    onOpenDetail: (() -> Unit)? = null
) {
    val entity = item.entity ?: return
    val currentTemp = entity.currentTemperature
    val isOff = entity.state == "off"
    val colors = rememberCardColors(active = !isOff, animationLabel = "climateCardBg")

    var localTarget by remember(entity.entityId) {
        mutableFloatStateOf((entity.targetTemperature ?: 21.0).toFloat())
    }
    val remoteTarget = entity.targetTemperature?.toFloat()
    LaunchedEffect(remoteTarget) {
        if (remoteTarget != null) localTarget = remoteTarget
    }

    DashboardCardShell(
        title = cardDisplayTitle(item),
        colors = colors,
        modifier = modifier,
        onOpenDetail = onOpenDetail
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CardStyle.Spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardIconBadge(icon = Icons.Rounded.Thermostat, tint = colors.onContainer)
            Spacer(modifier = Modifier.weight(1f))
            if (currentTemp != null) {
                Text(
                    text = "%.1f°".format(currentTemp),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onContainer
                )
            }
        }

        if (!isOff) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CardStyle.TightSpacing)
            ) {
                Text(
                    text = "Target",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        val newTemp = (localTarget - 0.5f).coerceIn(5f, 35f)
                        localTarget = newTemp
                        onAction(EntityAction.SetClimateTemperature(item.connectionId, item.entityId, newTemp.toDouble()))
                    },
                    modifier = Modifier.size(CardStyle.ControlIconButtonSize)
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease",
                        tint = colors.onContainer, modifier = Modifier.size(CardStyle.ControlIconSize))
                }
                Text(
                    text = "%.1f°".format(localTarget),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onContainer
                )
                IconButton(
                    onClick = {
                        val newTemp = (localTarget + 0.5f).coerceIn(5f, 35f)
                        localTarget = newTemp
                        onAction(EntityAction.SetClimateTemperature(item.connectionId, item.entityId, newTemp.toDouble()))
                    },
                    modifier = Modifier.size(CardStyle.ControlIconButtonSize)
                ) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Increase",
                        tint = colors.onContainer, modifier = Modifier.size(CardStyle.ControlIconSize))
                }
            }
        }

        if (entity.hvacModes.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(CardStyle.TightSpacing),
                verticalArrangement = Arrangement.spacedBy(CardStyle.TightSpacing)
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
