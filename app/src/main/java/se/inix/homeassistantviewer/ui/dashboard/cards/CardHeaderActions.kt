package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Trailing-slot icon group used by [DashboardCardShell]. Composed once here
 * so the rename pencil and history icon stay in a consistent layout — and
 * so cards that need only one of them (e.g. scripts / scenes don't get
 * history) don't have to do their own conditional layout.
 *
 * Either action may be null; an action is simply omitted when its
 * callback is.
 */
@Composable
internal fun CardHeaderActions(
    tint: Color,
    onRequestRename: (() -> Unit)?,
    onOpenDetail: (() -> Unit)?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (onRequestRename != null) {
            CardRenameAction(tint = tint, onClick = onRequestRename)
        }
        if (onOpenDetail != null) {
            CardHistoryAction(tint = tint, onClick = onOpenDetail)
        }
    }
}
