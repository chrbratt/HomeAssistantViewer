package se.inix.homeassistantviewer.ui.dashboard.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.ui.dashboard.cards.CardStyle

/**
 * Visible drag handle for reordering dashboard items. Long-press here to drag;
 * comparison selection uses the card header checkbox or a long-press on the title.
 */
@Composable
fun DragHandleIcon(
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Icons.Rounded.DragHandle,
        contentDescription = "Drag to reorder",
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
            .padding(end = 4.dp, bottom = 4.dp)
            .size(CardStyle.ActionIconSize)
    )
}
