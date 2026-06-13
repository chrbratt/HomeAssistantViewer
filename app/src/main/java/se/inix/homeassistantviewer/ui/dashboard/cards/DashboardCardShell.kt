package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import java.time.Instant

/**
 * Standard outer shell for every dashboard card. Encapsulates:
 *
 *  - the Material [Card] with the correct shape and container color,
 *  - the inner [Column] with [CardStyle.Padding] and [CardStyle.Spacing],
 *  - the [CardHeader] showing [title] on its own row at the top.
 *
 * The card body — sliders, switches, controls — lives in [content].
 *
 * When [onClick] is set, only the body is clickable so the header stays free
 * for comparison selection while selection mode is active.
 *
 * [timestamp] (when non-null) renders a subtle "time since last" footer; when
 * [isStale] is true the body is dimmed and the footer flips to an "outdated"
 * marker — the entity is momentarily unavailable but we keep showing its last
 * known value.
 */
@Composable
internal fun DashboardCardShell(
    title: String,
    colors: CardColors,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onOpenDetail: (() -> Unit)? = null,
    onRequestRename: (() -> Unit)? = null,
    comparisonSelection: ComparisonSelectionUi? = null,
    timestamp: Instant? = null,
    isStale: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val trailing: (@Composable () -> Unit)? =
        if (onOpenDetail == null && onRequestRename == null) null
        else { ->
            CardHeaderActions(
                tint = colors.onContainer,
                onRequestRename = onRequestRename,
                onOpenDetail = onOpenDetail
            )
        }

    val leading: (@Composable () -> Unit)? = comparisonSelection?.let { selection ->
        {
            AnimatedComparisonSelectionCheckbox(
                visible = selection.selectionModeActive,
                checked = selection.isSelected,
                onToggle = selection.onToggle,
                tint = colors.onContainer
            )
        }
    }

    val haptic = LocalHapticFeedback.current
    val headerModifier = comparisonSelection?.let { selection ->
        Modifier.pointerInput(selection.isSelected) {
            detectTapGestures(
                onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selection.onToggle()
                }
            )
        }
    } ?: Modifier

    val containerColors = CardDefaults.cardColors(containerColor = colors.container)
    val showSelectionOutline = comparisonSelection?.isSelected == true
    val cardModifier = modifier
        .fillMaxWidth()
        .then(
            if (showSelectionOutline) {
                Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.large
                )
            } else {
                Modifier
            }
        )

    Card(
        modifier = cardModifier,
        shape = MaterialTheme.shapes.large,
        colors = containerColors
    ) {
        Column(
            modifier = Modifier.padding(CardStyle.Padding),
            verticalArrangement = Arrangement.spacedBy(CardStyle.Spacing)
        ) {
            CardHeader(
                title = title,
                color = colors.onContainer,
                leading = leading,
                trailing = trailing,
                modifier = headerModifier.fillMaxWidth()
            )
            // The body (value/controls) is what goes stale; dim only it so the
            // title and the footer marker stay legible.
            val bodyModifier = Modifier
                .fillMaxWidth()
                .then(if (isStale) Modifier.alpha(STALE_BODY_ALPHA) else Modifier)
            if (onClick != null) {
                Column(
                    modifier = bodyModifier.clickable(onClick = onClick),
                    verticalArrangement = Arrangement.spacedBy(CardStyle.Spacing),
                    content = content
                )
            } else {
                Column(
                    modifier = bodyModifier,
                    verticalArrangement = Arrangement.spacedBy(CardStyle.Spacing),
                    content = content
                )
            }
            if (timestamp != null) {
                CardTimestampFooter(timestamp = timestamp, isStale = isStale)
            }
        }
    }
}

/** How much the card body fades while showing a stale (outdated) value. */
private const val STALE_BODY_ALPHA = 0.45f
