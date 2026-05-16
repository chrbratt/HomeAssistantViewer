package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import se.inix.homeassistantviewer.ui.dashboard.DashboardItem
import se.inix.homeassistantviewer.ui.dashboard.EntityAction

/**
 * Pure dispatcher that picks the right card for a dashboard entity based on
 * its domain. Cards live in this same package and are package-internal so
 * the dispatcher stays the single entry point.
 */
@Composable
fun EntityCard(
    item: DashboardItem.Entity,
    onAction: (EntityAction) -> Unit,
    onRequestRemove: () -> Unit,
    onOpenDetail: (connectionId: String, entityId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val entity = item.entity
    if (entity == null) {
        UnavailableEntityCard(
            entityId = item.entityId,
            onRequestRemove = onRequestRemove,
            modifier = modifier
        )
        return
    }

    // Scripts / scenes / automations are one-shot triggers with no useful
    // historical timeline ("did this script run?" is logged but not
    // chartable), so we skip the info icon on those cards.
    val openDetail: (() -> Unit)? = when (entity.domain) {
        "scene", "script", "automation" -> null
        else -> { -> onOpenDetail(item.connectionId, item.entityId) }
    }

    when (entity.domain) {
        "light", "switch", "input_boolean", "fan" ->
            ControlCard(item = item, onAction = onAction, onOpenDetail = openDetail, modifier = modifier)
        "cover" ->
            CoverCard(item = item, onAction = onAction, onOpenDetail = openDetail, modifier = modifier)
        "climate" ->
            ClimateCard(item = item, onAction = onAction, onOpenDetail = openDetail, modifier = modifier)
        "lock" ->
            LockCard(item = item, onAction = onAction, onOpenDetail = openDetail, modifier = modifier)
        "media_player" ->
            MediaPlayerCard(item = item, onAction = onAction, onOpenDetail = openDetail, modifier = modifier)
        "input_number" ->
            InputNumberCard(item = item, onAction = onAction, onOpenDetail = openDetail, modifier = modifier)
        // Scripts, scenes AND automations share the same "Run" card. A toggle for
        // an automation in HA flips its enabled state — almost never what the
        // user actually wants on a dashboard, so we use automation.trigger via
        // the Run button instead.
        "scene", "script", "automation" ->
            ActivateCard(item = item, onAction = onAction, modifier = modifier)
        else ->
            SensorCard(entity = entity, onOpenDetail = openDetail, modifier = modifier)
    }
}
