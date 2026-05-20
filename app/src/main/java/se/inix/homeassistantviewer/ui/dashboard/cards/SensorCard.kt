package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import se.inix.homeassistantviewer.ui.common.BinaryStateIndicator
import se.inix.homeassistantviewer.ui.common.NumericValueDisplay
import se.inix.homeassistantviewer.ui.common.formatSensorValue
import se.inix.homeassistantviewer.ui.common.getIconForEntity
import se.inix.homeassistantviewer.ui.common.sensorIconTint
import se.inix.homeassistantviewer.ui.dashboard.DashboardItem

/** Read-only display card for sensors and anything else without a control affordance. */
@Composable
internal fun SensorCard(
    item: DashboardItem.Entity,
    modifier: Modifier = Modifier,
    onOpenDetail: (() -> Unit)? = null
) {
    val entity = item.entity ?: return
    val icon = getIconForEntity(entity)
    val iconTint = sensorIconTint(entity)
    val unit = entity.unitOfMeasurement
    val isBinary = entity.domain == "binary_sensor" && unit == null &&
        (entity.state == "on" || entity.state == "off")
    val colors = rememberCardColors(active = false, animationLabel = "sensorCardBg")

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
            CardIconBadge(
                icon = icon,
                tint = iconTint,
                backgroundColor = iconTint.copy(alpha = 0.15f)
            )
            Spacer(modifier = Modifier.weight(1f))
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
