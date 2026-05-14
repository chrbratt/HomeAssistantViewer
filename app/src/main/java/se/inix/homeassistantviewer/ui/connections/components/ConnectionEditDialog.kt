package se.inix.homeassistantviewer.ui.connections.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import se.inix.homeassistantviewer.data.ha.AddressProbeResult
import se.inix.homeassistantviewer.data.ha.ApiProbeResult

/**
 * Edit-or-add dialog for a Home Assistant connection.
 *
 * Validation rules:
 *  - Inline URL error only shows after the user has tried to save (otherwise an
 *    empty initial value would flash red).
 *  - Save is enabled only when both URL and token are non-blank; final URL
 *    validity is enforced inside [onSave].
 */
@Composable
internal fun ConnectionEditDialog(
    isNew: Boolean,
    initial: ConnectionEditState,
    addressResult: AddressProbeResult?,
    apiResult: ApiProbeResult?,
    isTesting: Boolean,
    urlValidator: (String) -> Boolean,
    onTest: (String, String) -> Unit,
    onSave: (name: String, url: String, token: String) -> Boolean,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var url by remember { mutableStateOf(initial.baseUrl) }
    var token by remember { mutableStateOf(initial.token) }
    var showHelp by remember { mutableStateOf(false) }
    var showToken by remember { mutableStateOf(false) }
    var attemptedSave by remember { mutableStateOf(false) }

    val urlValid by remember(url) { derivedStateOf { urlValidator(url) } }
    val showUrlError = attemptedSave && url.isNotBlank() && !urlValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add connection" else "Edit connection") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("My Home") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Base URL") },
                    placeholder = { Text("https://your-ha.duckdns.org") },
                    singleLine = true,
                    isError = showUrlError,
                    supportingText = if (showUrlError) {
                        { Text("Enter a valid URL, e.g. https://homeassistant.local:8123") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Long-Lived Access Token") },
                    singleLine = true,
                    visualTransformation = if (showToken) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showToken = !showToken }) {
                            Icon(
                                imageVector = if (showToken) Icons.Rounded.VisibilityOff
                                              else Icons.Rounded.Visibility,
                                contentDescription = if (showToken) "Hide token" else "Show token"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ConnectionResultRow(
                            icon = Icons.Rounded.Link,
                            label = "Server",
                            result = addressResult,
                            isLoading = isTesting
                        )
                        ConnectionResultRow(
                            icon = Icons.Rounded.VpnKey,
                            label = "API",
                            result = apiResult,
                            isLoading = isTesting
                        )
                        OutlinedButton(
                            onClick = { onTest(url, token) },
                            enabled = !isTesting && url.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (isTesting) "Testing…" else "Test connection")
                        }
                    }
                }

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Help,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "How to connect",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showHelp = !showHelp },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    if (showHelp) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = if (showHelp) "Collapse" else "Expand",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (showHelp) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                "Getting your access token:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "1. Open Home Assistant in a browser\n" +
                                "2. Click your profile picture (bottom-left sidebar)\n" +
                                "3. Scroll down to \"Long-Lived Access Tokens\"\n" +
                                "4. Click \"Create Token\", give it a name\n" +
                                "5. Copy the token and paste it above",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "WebSocket API is enabled by default in all modern Home Assistant installations. No extra configuration is needed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    attemptedSave = true
                    if (urlValid) onSave(name, url, token)
                },
                enabled = url.isNotBlank() && token.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
