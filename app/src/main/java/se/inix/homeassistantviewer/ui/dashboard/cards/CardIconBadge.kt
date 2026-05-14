package se.inix.homeassistantviewer.ui.dashboard.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Circular icon badge used at the leading edge of every dashboard card row.
 * One place to tweak size, shape, or contrast of every entity glyph.
 */
@Composable
internal fun CardIconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    backgroundColor: Color = tint.copy(alpha = 0.12f),
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(CardStyle.IconBoxSize)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(CardStyle.IconSize)
        )
    }
}
