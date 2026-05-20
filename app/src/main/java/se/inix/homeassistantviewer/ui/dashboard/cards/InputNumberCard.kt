package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sensors
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.domain.entity.inputNumberMax
import se.inix.homeassistantviewer.domain.entity.inputNumberMin
import se.inix.homeassistantviewer.domain.entity.inputNumberStep
import se.inix.homeassistantviewer.domain.entity.inputNumberValue
import se.inix.homeassistantviewer.ui.common.formatSensorValue
import se.inix.homeassistantviewer.ui.dashboard.DashboardItem
import se.inix.homeassistantviewer.ui.dashboard.EntityAction

@Composable
internal fun InputNumberCard(
    item: DashboardItem.Entity,
    onAction: (EntityAction) -> Unit,
    modifier: Modifier = Modifier,
    onOpenDetail: (() -> Unit)? = null
) {
    val entity = item.entity ?: return
    val currentValue = entity.inputNumberValue ?: return
    val colors = rememberCardColors(active = true, animationLabel = "inputNumberCardBg")

    var isDragging by remember { mutableStateOf(false) }
    var localValue by remember(entity.entityId) { mutableFloatStateOf(currentValue.toFloat()) }
    LaunchedEffect(currentValue, isDragging) {
        if (!isDragging) localValue = currentValue.toFloat()
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
            CardIconBadge(icon = Icons.Rounded.Sensors, tint = colors.onContainer)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatSensorValue(localValue.toString()),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onContainer
            )
        }

        CardSlider(
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
    }
}
