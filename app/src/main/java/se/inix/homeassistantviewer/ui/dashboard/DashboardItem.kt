package se.inix.homeassistantviewer.ui.dashboard

import se.inix.homeassistantviewer.data.model.HaEntityState

/** An item on the dashboard — either an entity card or a row-break divider. */
sealed class DashboardItem {
    abstract val key: String

    /**
     * @param customName user-given display name overriding HA's `friendly_name`.
     *                   See [se.inix.homeassistantviewer.data.model.FavoriteItem.Entity].
     */
    data class Entity(
        val connectionId: String,
        val entityId: String,
        val entity: HaEntityState?,
        val customName: String? = null
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
