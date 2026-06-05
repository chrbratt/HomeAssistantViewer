package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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

/** Shown on comparable cards while comparison selection mode is active. */
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

@Composable
internal fun AnimatedComparisonSelectionCheckbox(
    visible: Boolean,
    checked: Boolean,
    onToggle: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally(),
        modifier = modifier
    ) {
        ComparisonSelectionCheckbox(
            checked = checked,
            onToggle = onToggle,
            tint = tint
        )
    }
}
