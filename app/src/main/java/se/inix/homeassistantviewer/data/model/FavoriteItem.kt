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

    /**
     * @param customName user-given display name overriding HA's `friendly_name`.
     *                   `null` means "use the HA-provided name". Empty strings
     *                   are not permitted — callers normalise to `null` first.
     */
    data class Entity(
        val connectionId: String,
        val entityId: String,
        val customName: String? = null
    ) : FavoriteItem() {
        override val key: String get() = "e:$connectionId/$entityId"
    }

    /**
     * @param title optional section heading rendered on the divider line.
     *              `null` (or blank) means "plain horizontal line, no label".
     */
    data class Divider(
        val id: String,
        val title: String? = null
    ) : FavoriteItem() {
        override val key: String get() = "d:$id"
    }
}
