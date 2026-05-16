package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/**
 * Standard title row used at the top of every dashboard card. Putting the name
 * on its own line lets long entity names use the full card width instead of
 * fighting with the icon + switch for the same horizontal space.
 *
 * Caller decides the color so light/dark/active states stay consistent with
 * the surrounding [Card] surface.
 *
 * The optional [trailing] slot is used by every entity card to show a small
 * "open history" icon. Keeping it on the shared header means future style
 * tweaks (size, color, icon choice) happen in one file rather than eight.
 */
@Composable
internal fun CardHeader(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    if (trailing == null) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            trailing()
        }
    }
}
