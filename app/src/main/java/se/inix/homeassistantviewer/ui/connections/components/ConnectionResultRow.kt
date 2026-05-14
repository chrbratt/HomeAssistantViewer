package se.inix.homeassistantviewer.ui.connections.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.data.ha.AddressProbeResult
import se.inix.homeassistantviewer.data.ha.ApiProbeResult

/**
 * Single line of the connection-test panel ("Server" / "API"). Renders an
 * icon, a label and a status based on the heterogeneous result types.
 */
@Composable
internal fun ConnectionResultRow(
    icon: ImageVector,
    label: String,
    result: Any?,
    isLoading: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(48.dp))
        when {
            isLoading && result == null -> {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Text("Checking…", style = MaterialTheme.typography.bodySmall)
            }
            result is AddressProbeResult.Reachable    -> StatusText(true,  "HTTP ${result.httpCode}")
            result is AddressProbeResult.Unreachable  -> StatusText(false, result.detail.take(60))
            result is AddressProbeResult.InvalidInput -> StatusText(false, result.detail)
            result is ApiProbeResult.Ok               -> StatusText(true,  "OK")
            result is ApiProbeResult.HttpError        -> StatusText(false, "HTTP ${result.code}")
            result is ApiProbeResult.NetworkError     -> StatusText(false, result.detail.take(60))
            result is ApiProbeResult.Skipped ->
                Text(result.reason, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            else ->
                Text("—", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusText(ok: Boolean, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
        )
    }
}
