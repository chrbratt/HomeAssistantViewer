package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Small trailing button used inside [CardHeader] to open the entity-detail
 * screen. Lives in one place so the icon choice / size stays consistent
 * across every card.
 *
 * Sized down to match the compact card layout introduced in [CardStyle]
 * — a default `IconButton` would make the header chunky.
 */
@Composable
internal fun CardHistoryAction(
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(28.dp),
        colors = IconButtonDefaults.iconButtonColors(contentColor = tint)
    ) {
        Icon(
            imageVector = Icons.Rounded.Timeline,
            contentDescription = "Open history",
            modifier = Modifier.size(16.dp)
        )
    }
}
