package se.inix.homeassistantviewer.ui.dashboard.cards

import se.inix.homeassistantviewer.ui.dashboard.DashboardItem

/**
 * Resolves the display title for an entity card.
 *
 * Order of precedence:
 *  1. User-provided [DashboardItem.Entity.customName] — explicit user intent
 *     always wins; if the user renamed it, they want their name shown.
 *  2. HA's `friendly_name` (when the entity has loaded).
 *  3. The raw `entityId` as a last resort (when even HA hasn't responded
 *     yet — better than rendering an empty header).
 *
 * Centralising this avoids the same three-line fallback being duplicated in
 * each of the eight domain cards — any future rule change (e.g. "strip the
 * `domain.` prefix from entity-id fallbacks") happens here, once.
 */
internal fun cardDisplayTitle(item: DashboardItem.Entity): String =
    item.customName?.takeUnless { it.isBlank() }
        ?: item.entity?.friendlyName
        ?: item.entityId
