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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

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
                visible = true,
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
            if (onClick != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onClick),
                    verticalArrangement = Arrangement.spacedBy(CardStyle.Spacing),
                    content = content
                )
            } else {
                content()
            }
        }
    }
}
