package se.inix.homeassistantviewer.ui.dashboard

import se.inix.homeassistantviewer.data.model.HaEntityState
import se.inix.homeassistantviewer.data.settings.CardTimestamp

/** An item on the dashboard — either an entity card or a row-break divider. */
sealed class DashboardItem {
    abstract val key: String

    /**
     * @param entity the last *displayable* (good) state — retained even while
     *               the entity is temporarily [isStale], so the card can keep
     *               showing the last known value instead of going blank.
     * @param customName user-given display name overriding HA's `friendly_name`.
     *                   See [se.inix.homeassistantviewer.data.model.FavoriteItem.Entity].
     * @param timestampMode the already-resolved card-timestamp choice (per-entity
     *                   override or the global default).
     * @param isStale true when HA currently reports this entity as
     *                   `unavailable`/`unknown` but we still hold a last good value.
     */
    data class Entity(
        val connectionId: String,
        val entityId: String,
        val entity: HaEntityState?,
        val customName: String? = null,
        val timestampMode: CardTimestamp = CardTimestamp.NONE,
        val isStale: Boolean = false
    ) : DashboardItem() {
        override val key: String get() = "e:$connectionId/$entityId"
    }

    /** @param title optional section heading rendered on the divider line. */
    data class Divider(
        val id: String,
        val title: String? = null
    ) : DashboardItem() {
        override val key: String get() = "d:$id"
    }
}
