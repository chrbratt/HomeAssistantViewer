package se.inix.homeassistantviewer.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Full-width visual row break inserted between dashboard cards so the user
 * can force symmetry when cards vary in height.
 *
 * **Title:** when [title] is non-blank the divider doubles as a section
 * heading — the title is rendered between two horizontal rules. Tapping
 * the divider invokes [onEditTitle] so the user can name (or clear) the
 * section without leaving the dashboard. When [title] is null the divider
 * stays as a thin pill — same behaviour as before titles existed.
 */
@Composable
fun RowBreakDivider(
    title: String?,
    onEditTitle: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val ruleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            // The whole divider is the tap-target for editing. We rely on
            // `clickable` here rather than wrapping each piece — the user
            // shouldn't have to aim at a tiny text area.
            .clickable(onClick = onEditTitle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Rounded.DragHandle,
            contentDescription = null,
            tint = labelColor.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )

        if (title != null && title.isNotBlank()) {
            // Title sandwiched between two rules — visually obvious as a
            // section heading without any extra chrome.
            HorizontalRule(modifier = Modifier.weight(1f), color = ruleColor)
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            HorizontalRule(modifier = Modifier.weight(1f), color = ruleColor)
        } else {
            HorizontalRule(modifier = Modifier.weight(1f), color = ruleColor)
        }

        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Remove row break",
                tint = labelColor.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Single thin pill used inside [RowBreakDivider]. Pulled out because the
 * titled variant draws it twice (left + right of the title), and we want
 * both halves to share the exact same styling.
 */
@Composable
private fun HorizontalRule(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = modifier
            .height(2.dp)
            .background(color = color, shape = RoundedCornerShape(50))
    )
}
