package se.inix.homeassistantviewer.data.settings

import se.inix.homeassistantviewer.data.model.ComparisonEntity

/**
 * Serialises the comparison selection set stored in DataStore.
 * Format: comma-separated entity keys (`e:<connId>/<entityId>`).
 */
internal object ComparisonSelectionCodec {

    fun serialize(selection: Set<ComparisonEntity>): String =
        selection.joinToString(",") { it.key }

    fun deserialize(raw: String): Set<ComparisonEntity> =
        raw.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { parseKey(it) }
            .toSet()

    private fun parseKey(key: String): ComparisonEntity? {
        if (!key.startsWith("e:")) return null
        val body = key.removePrefix("e:")
        val slash = body.indexOf('/')
        if (slash <= 0 || slash >= body.length - 1) return null
        val connectionId = body.substring(0, slash)
        val entityId = body.substring(slash + 1)
        if (connectionId.isBlank() || entityId.isBlank()) return null
        return ComparisonEntity(connectionId, entityId)
    }
}
