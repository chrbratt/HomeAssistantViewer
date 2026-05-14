package se.inix.homeassistantviewer.data.model

/**
 * One slot on the dashboard. Either an actual Home Assistant entity, or a
 * full-width divider the user can insert to break a row in the staggered grid.
 *
 * The [key] is stable and unique across reorderings and is used as the
 * `LazyColumn`/`LazyVerticalStaggeredGrid` item key.
 */
sealed class FavoriteItem {
    abstract val key: String

    data class Entity(
        val connectionId: String,
        val entityId: String
    ) : FavoriteItem() {
        override val key: String get() = "e:$connectionId/$entityId"
    }

    data class Divider(val id: String) : FavoriteItem() {
        override val key: String get() = "d:$id"
    }
}
