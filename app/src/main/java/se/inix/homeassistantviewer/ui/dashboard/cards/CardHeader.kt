package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
 * The optional [leading] slot holds the comparison-selection checkbox when
 * active — it animates in horizontally so the title shifts rather than
 * being covered by an overlay.
 *
 * The entity name is a label, not data, so it is rendered at a lower
 * emphasis than the value the card presents (see [TITLE_EMPHASIS_ALPHA]) —
 * this establishes a clear "muted name / strong value" hierarchy instead of
 * the two reading as the same shade.
 */
@Composable
internal fun CardHeader(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val titleColor = color.copy(alpha = color.alpha * TITLE_EMPHASIS_ALPHA)
    if (leading == null && trailing == null) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = titleColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CardStyle.TightSpacing)
        ) {
            leading?.invoke()
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
        }
    }
}

/**
 * Medium-emphasis alpha for the entity name. Mirrors Material's
 * medium-emphasis text guidance so the name sits visibly below the value
 * without becoming hard to read.
 */
private const val TITLE_EMPHASIS_ALPHA = 0.7f
