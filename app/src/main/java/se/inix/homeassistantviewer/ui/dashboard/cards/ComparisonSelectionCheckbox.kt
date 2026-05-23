package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Shown inline in the card header only on **selected** cards. Tap to deselect;
 * long-press on the card body also toggles selection.
 */
@Composable
internal fun ComparisonSelectionCheckbox(
    visible: Boolean,
    checked: Boolean,
    onToggle: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && checked,
        enter = fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally(),
        modifier = modifier
    ) {
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(CardStyle.ActionIconButtonSize),
            colors = IconButtonDefaults.iconButtonColors(contentColor = tint)
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Deselect for comparison",
                modifier = Modifier.size(CardStyle.ActionIconSize)
            )
        }
    }
}
