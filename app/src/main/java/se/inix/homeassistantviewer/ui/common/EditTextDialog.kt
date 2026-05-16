package se.inix.homeassistantviewer.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Generic single-field text-edit dialog. One implementation is reused for
 * "rename favourite" and "set section title" since both reduce to the same
 * UX: a label, a single line of text, save / cancel.
 *
 * **Empty-as-clear semantics:** when the user saves an empty (or
 * whitespace-only) value, [onConfirm] receives `null`. Callers persist
 * `null` as "no override" — i.e. fall back to HA's `friendly_name` for
 * entities, or render a plain divider line for dividers.
 *
 * The field auto-focuses and pops the keyboard so the user can start typing
 * immediately — these dialogs are *only* opened with intent to edit, so
 * making them tap-then-tap-again is friction with no upside.
 */
@Composable
fun EditTextDialog(
    title: String,
    label: String,
    initialValue: String,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
    placeholder: String? = null,
    confirmLabel: String = "Save",
    supportingText: String? = null
) {
    var text by remember { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // Focus once the dialog has fully composed. Without the small delay the
    // request races the dialog's enter-animation and the soft keyboard
    // doesn't come up reliably on first open.
    val onPositioned = remember {
        {
            scope.launch {
                delay(80L)
                runCatching { focusRequester.requestFocus() }
            }
            Unit
        }
    }

    val submit: () -> Unit = {
        onConfirm(text.trim().takeIf { it.isNotEmpty() })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(label) },
                    placeholder = placeholder?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )
                supportingText?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Side-effect after composition completes. AlertDialog has no
            // built-in `onShown` hook, so we piggy-back on first composition
            // of the body slot.
            onPositioned()
        },
        confirmButton = { Button(onClick = submit) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
