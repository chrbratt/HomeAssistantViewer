package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

/**
 * The container + onContainer color pair used by every dashboard card.
 *
 * Holding both as a single value object lets a card pass one parameter to its
 * inner composables instead of two, and keeps the meaning of "card color" in
 * one place — so a future redesign that, say, introduces a third "warning"
 * variant becomes a local change to this file instead of touching every card.
 */
internal data class CardColors(
    val container: Color,
    val onContainer: Color
)

/**
 * Standard active/inactive color pair used by every interactive card. Returns
 * a [CardColors] whose container animates between [activeContainer] and the
 * neutral surfaceVariant so state changes feel responsive.
 *
 * Defaults follow Material's primary/surface roles; callers can override for
 * domains that need a non-primary "on" color (e.g. a Lock card's error red).
 */
@Composable
internal fun rememberCardColors(
    active: Boolean,
    activeContainer: Color = MaterialTheme.colorScheme.primaryContainer,
    activeOnContainer: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    inactiveContainer: Color = MaterialTheme.colorScheme.surfaceVariant,
    inactiveOnContainer: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    animationLabel: String = "cardContainer"
): CardColors {
    val container by animateColorAsState(
        targetValue = if (active) activeContainer else inactiveContainer,
        animationSpec = spring(),
        label = animationLabel
    )
    val onContainer = if (active) activeOnContainer else inactiveOnContainer
    return CardColors(container = container, onContainer = onContainer)
}
