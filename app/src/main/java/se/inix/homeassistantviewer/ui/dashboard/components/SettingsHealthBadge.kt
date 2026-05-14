package se.inix.homeassistantviewer.ui.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import se.inix.homeassistantviewer.ui.dashboard.ConnectionHealth

/**
 * Settings icon with a small color-coded dot communicating overall
 * connection health (green / amber / red). Complements the transient
 * top-of-screen status banner with a persistent, always-visible cue.
 *
 * The dot color is animated so the user perceives state changes as a
 * smooth shift rather than a jarring flicker between transient states.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsHealthBadge(
    health: ConnectionHealth,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dotColor by animateColorAsState(
        targetValue = health.dotColor,
        animationSpec = spring(),
        label = "settingsHealthDot"
    )
    IconButton(onClick = onClick, modifier = modifier) {
        BadgedBox(
            badge = { Badge(containerColor = dotColor) }
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Settings — ${health.description}"
            )
        }
    }
}
