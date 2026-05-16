package se.inix.homeassistantviewer.data.settings

import se.inix.homeassistantviewer.data.model.FavoriteItem
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Pure serialization codec for the favourites list stored in DataStore.
 *
 * Pulled out of [FavoritesStore] so the format (which has subtle
 * backward-compatibility rules) can be unit-tested without spinning up a
 * DataStore + scope.
 *
 * **Format** (comma-separated tokens). Each token is one of:
 *
 *  - `e:<connId>|<entityId>`                       — entity favourite
 *  - `e:<connId>|<entityId>|<urlEncoded-name>`     — renamed favourite
 *  - `d:<uuid>`                                    — plain divider
 *  - `d:<uuid>|<urlEncoded-title>`                 — divider with heading
 *
 * The `e:` / `d:` discriminator is what distinguishes a *renamed entity*
 * (also has one `|`) from a *titled divider* (also has one `|`) — without
 * the prefix the two would collide.
 *
 * URL-encoding the variable string fields lets users freely use `,`, `|`
 * or `%` in their custom names without breaking the outer split / inner
 * pipe-split layers.
 *
 * **Legacy compatibility:** payloads written before the prefix existed
 * are still readable — entries with `|` are treated as old entity
 * favourites, entries without `|` as old (untitled) dividers. The next
 * write upgrades them to the new format transparently.
 */
internal object FavoritesCodec {

    fun serialize(list: List<FavoriteItem>): String =
        list.joinToString(",") { item ->
            when (item) {
                is FavoriteItem.Entity -> {
                    val core = "e:${item.connectionId}|${item.entityId}"
                    val name = item.customName
                    if (name.isNullOrBlank()) core
                    else "$core|${URLEncoder.encode(name, "UTF-8")}"
                }
                is FavoriteItem.Divider -> {
                    val core = "d:${item.id}"
                    val title = item.title
                    if (title.isNullOrBlank()) core
                    else "$core|${URLEncoder.encode(title, "UTF-8")}"
                }
            }
        }

    fun deserialize(raw: String): List<FavoriteItem> =
        raw.split(",").filter { it.isNotBlank() }.mapNotNull { token ->
            when {
                token.startsWith("e:") -> parseEntity(token.removePrefix("e:"))
                token.startsWith("d:") -> parseDivider(token.removePrefix("d:"))
                // Legacy: no discriminator — disambiguate by structure.
                token.contains('|') -> parseEntity(token)
                else -> FavoriteItem.Divider(id = token)
            }
        }

    private fun parseEntity(body: String): FavoriteItem.Entity? {
        val parts = body.split("|", limit = 3)
        if (parts.size < 2 || parts[0].isBlank() || parts[1].isBlank()) return null
        val customName = parts.getOrNull(2)?.let { decodeOrNull(it) }
        return FavoriteItem.Entity(
            connectionId = parts[0],
            entityId = parts[1],
            customName = customName?.takeUnless { it.isBlank() }
        )
    }

    private fun parseDivider(body: String): FavoriteItem.Divider? {
        val parts = body.split("|", limit = 2)
        if (parts[0].isBlank()) return null
        val title = parts.getOrNull(1)?.let { decodeOrNull(it) }
        return FavoriteItem.Divider(
            id = parts[0],
            title = title?.takeUnless { it.isBlank() }
        )
    }

    /** Tolerant decode — a malformed token shouldn't take down the whole list. */
    private fun decodeOrNull(encoded: String): String? =
        runCatching { URLDecoder.decode(encoded, "UTF-8") }.getOrNull()
}
