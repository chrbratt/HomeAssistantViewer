package se.inix.homeassistantviewer.data.model

/**
 * A dashboard entity marked for the comparison graph view.
 * Uses the same stable key format as [FavoriteItem.Entity] so the two
 * concepts can share lookup logic without coupling persistence.
 */
data class ComparisonEntity(
    val connectionId: String,
    val entityId: String
) {
    val key: String get() = "e:$connectionId/$entityId"
}

/** Domains with no plottable history — excluded from comparison selection. */
val NON_COMPARABLE_DOMAINS = setOf("scene", "script", "automation")

fun ComparisonEntity.isComparableDomain(): Boolean =
    entityId.substringBefore('.') !in NON_COMPARABLE_DOMAINS
