package se.inix.homeassistantviewer.ui.dashboard.cards

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

/**
 * Standard outer shell for every dashboard card. Encapsulates:
 *
 *  - the Material [Card] with the correct shape and container color,
 *  - the inner [Column] with [CardStyle.Padding] and [CardStyle.Spacing],
 *  - the [CardHeader] showing [title] on its own row at the top.
 *
 * The card body — sliders, switches, controls — lives in [content].
 *
 * A tap-handling overload exists ([onClick] != null) for cards whose surface
 * is its own primary action (Lock, Control). For cards where the body has
 * multiple actions (Cover with up/stop/down) pass null and let inner controls
 * dispatch their own events.
 */
@Composable
internal fun DashboardCardShell(
    title: String,
    colors: CardColors,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onOpenDetail: (() -> Unit)? = null,
    onRequestRename: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    // Build a single trailing slot that holds zero, one or both action
    // icons. Keeping the composition here means each domain card just
    // passes the two callbacks and never has to lay out its own icons.
    val trailing: (@Composable () -> Unit)? =
        if (onOpenDetail == null && onRequestRename == null) null
        else { ->
            CardHeaderActions(
                tint = colors.onContainer,
                onRequestRename = onRequestRename,
                onOpenDetail = onOpenDetail
            )
        }
    val body: @Composable ColumnScope.() -> Unit = {
        CardHeader(
            title = title,
            color = colors.onContainer,
            trailing = trailing
        )
        content()
    }
    val containerColors = CardDefaults.cardColors(containerColor = colors.container)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = containerColors
        ) {
            Column(
                modifier = Modifier.padding(CardStyle.Padding),
                verticalArrangement = Arrangement.spacedBy(CardStyle.Spacing),
                content = body
            )
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = containerColors
        ) {
            Column(
                modifier = Modifier.padding(CardStyle.Padding),
                verticalArrangement = Arrangement.spacedBy(CardStyle.Spacing),
                content = body
            )
        }
    }
}
