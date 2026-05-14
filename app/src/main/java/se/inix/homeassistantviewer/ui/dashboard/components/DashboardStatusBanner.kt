package se.inix.homeassistantviewer.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.ui.dashboard.DashboardStatusBar

/**
 * Surface banner that informs the user about connection / freshness state. The
 * [DashboardStatusBar.Hidden] case folds away via [AnimatedVisibility] so the
 * banner takes no space once everything is healthy and up-to-date.
 */
@Composable
fun DashboardStatusBanner(
    status: DashboardStatusBar,
    onOpenSettings: () -> Unit
) {
    AnimatedVisibility(
        visible = status !is DashboardStatusBar.Hidden,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val style = bannerStyle(status)
        Surface(color = style.background, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    style.showProgress -> CircularProgressIndicator(
                        color = style.foreground,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp)
                    )
                    style.showCheck -> Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = style.foreground,
                        modifier = Modifier.size(16.dp)
                    )
                    else -> Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = style.foreground,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = style.text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = style.foreground,
                    modifier = Modifier.weight(1f)
                )
                if (style.showSettingsAction) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text("Settings", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

private data class BannerStyle(
    val background: Color,
    val foreground: Color,
    val text: String,
    val showProgress: Boolean,
    val showCheck: Boolean,
    val showSettingsAction: Boolean
)

@Composable
private fun bannerStyle(status: DashboardStatusBar): BannerStyle {
    val colorScheme = MaterialTheme.colorScheme
    return when (status) {
        is DashboardStatusBar.Hidden -> BannerStyle(
            background = Color.Transparent,
            foreground = colorScheme.onSurface,
            text = "",
            showProgress = false,
            showCheck = false,
            showSettingsAction = false
        )
        is DashboardStatusBar.Connecting -> BannerStyle(
            background = colorScheme.secondaryContainer,
            foreground = colorScheme.onSecondaryContainer,
            text = status.text,
            showProgress = true,
            showCheck = false,
            showSettingsAction = false
        )
        is DashboardStatusBar.Refreshing -> BannerStyle(
            background = colorScheme.secondaryContainer,
            foreground = colorScheme.onSecondaryContainer,
            text = status.text,
            showProgress = true,
            showCheck = false,
            showSettingsAction = false
        )
        is DashboardStatusBar.Ready -> BannerStyle(
            background = colorScheme.primaryContainer,
            foreground = colorScheme.onPrimaryContainer,
            text = status.text,
            showProgress = false,
            showCheck = true,
            showSettingsAction = false
        )
        is DashboardStatusBar.Warning -> BannerStyle(
            background = colorScheme.tertiaryContainer,
            foreground = colorScheme.onTertiaryContainer,
            text = status.text,
            showProgress = false,
            showCheck = false,
            showSettingsAction = false
        )
        is DashboardStatusBar.Error -> BannerStyle(
            background = colorScheme.errorContainer,
            foreground = colorScheme.onErrorContainer,
            text = status.text,
            showProgress = false,
            showCheck = false,
            showSettingsAction = true
        )
    }
}
