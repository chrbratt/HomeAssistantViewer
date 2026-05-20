package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Sibling of [CardHistoryAction] used to trigger the rename dialog for an
 * entity favourite. Same size / colour treatment so both icons in the
 * trailing slot stay visually balanced.
 */
@Composable
internal fun CardRenameAction(
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(CardStyle.ActionIconButtonSize),
        colors = IconButtonDefaults.iconButtonColors(contentColor = tint)
    ) {
        Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = "Rename",
            modifier = Modifier.size(CardStyle.ActionIconSize)
        )
    }
}
