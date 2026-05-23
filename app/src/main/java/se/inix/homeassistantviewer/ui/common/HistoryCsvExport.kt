package se.inix.homeassistantviewer.ui.common

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import se.inix.homeassistantviewer.domain.history.HISTORY_CSV_MIME_TYPE

@Composable
fun rememberHistoryCsvExportLauncher(
    suggestedFileName: () -> String,
    onExportToUri: (ContentResolver, Uri) -> Unit
): () -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(HISTORY_CSV_MIME_TYPE)
    ) { uri ->
        if (uri != null) {
            onExportToUri(context.contentResolver, uri)
        }
    }
    return { launcher.launch(suggestedFileName()) }
}
