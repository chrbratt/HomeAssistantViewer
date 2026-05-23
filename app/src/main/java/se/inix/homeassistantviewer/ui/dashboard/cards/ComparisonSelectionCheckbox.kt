package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Always visible on comparable cards. Tap to toggle comparison selection;
 * long-press the card title row for the same action.
 */
@Composable
internal fun ComparisonSelectionCheckbox(
    checked: Boolean,
    onToggle: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier.size(CardStyle.ActionIconButtonSize),
        colors = IconButtonDefaults.iconButtonColors(contentColor = tint)
    ) {
        Icon(
            imageVector = if (checked) {
                Icons.Rounded.CheckCircle
            } else {
                Icons.Rounded.RadioButtonUnchecked
            },
            contentDescription = if (checked) {
                "Deselect for comparison"
            } else {
                "Select for comparison"
            },
            modifier = Modifier.size(CardStyle.ActionIconSize)
        )
    }
}
