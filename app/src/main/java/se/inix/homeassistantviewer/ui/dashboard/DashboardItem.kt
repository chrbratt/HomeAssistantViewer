package se.inix.homeassistantviewer.ui.dashboard

import se.inix.homeassistantviewer.data.model.HaEntityState

/** An item on the dashboard — either an entity card or a row-break divider. */
sealed class DashboardItem {
    abstract val key: String

    data class Entity(
        val connectionId: String,
        val entityId: String,
        val entity: HaEntityState?
    ) : DashboardItem() {
        override val key: String get() = "e:$connectionId/$entityId"
    }

    data class Divider(val id: String) : DashboardItem() {
        override val key: String get() = "d:$id"
    }
}
