package se.inix.homeassistantviewer.ui.dashboard.components

import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import se.inix.homeassistantviewer.ui.dashboard.DashboardItem

/**
 * Resolves the grid span for the item at [index] in [items].
 *
 *  - Dividers always take a full row (forces a row break).
 *  - An entity that is the *only* entity in its section (the range between the
 *    surrounding dividers, or list edges) is stretched to a full row too. This
 *    keeps the layout from looking lopsided when a user manually inserts a
 *    divider after a single entity in a 2- or 3-column grid.
 *  - Otherwise the entity uses one lane and lays out normally.
 *
 * Pure function — depends only on its inputs, deliberately testable.
 */
internal fun dashboardSpan(items: List<DashboardItem>, index: Int): StaggeredGridItemSpan {
    if (index !in items.indices) return StaggeredGridItemSpan.SingleLane
    if (items[index] is DashboardItem.Divider) return StaggeredGridItemSpan.FullLine

    var sectionStart = 0
    for (i in index - 1 downTo 0) {
        if (items[i] is DashboardItem.Divider) { sectionStart = i + 1; break }
    }

    var sectionEndExclusive = items.size
    for (i in index + 1 until items.size) {
        if (items[i] is DashboardItem.Divider) { sectionEndExclusive = i; break }
    }

    val entitiesInSection = (sectionStart until sectionEndExclusive)
        .count { items[it] is DashboardItem.Entity }

    return if (entitiesInSection == 1) StaggeredGridItemSpan.FullLine
    else StaggeredGridItemSpan.SingleLane
}
