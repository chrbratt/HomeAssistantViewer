package se.inix.homeassistantviewer.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import se.inix.homeassistantviewer.data.backup.BACKUP_FILE_SUFFIX
import se.inix.homeassistantviewer.data.backup.BackupCodec
import se.inix.homeassistantviewer.data.backup.BackupImporter
import se.inix.homeassistantviewer.data.backup.BackupParseException
import se.inix.homeassistantviewer.data.backup.InternalSnapshotStore
import se.inix.homeassistantviewer.data.settings.ColorPalette
import se.inix.homeassistantviewer.data.settings.Density
import se.inix.homeassistantviewer.data.settings.SettingsRepository
import se.inix.homeassistantviewer.data.settings.ThemeMode
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupCodec: BackupCodec,
    private val backupImporter: BackupImporter,
    private val internalSnapshotStore: InternalSnapshotStore,
) : ViewModel() {

    val dashboardColumns: StateFlow<Int> = settingsRepository.dashboardColumns
    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
    val colorPalette: StateFlow<ColorPalette> = settingsRepository.colorPalette
    val density: StateFlow<Density> = settingsRepository.density

    private val _internalSnapshots =
        kotlinx.coroutines.flow.MutableStateFlow(internalSnapshotStore.list())
    val internalSnapshots: StateFlow<List<InternalSnapshotStore.SnapshotMeta>> =
        _internalSnapshots.asStateFlow()

    sealed class BackupFeedback {
        data class Success(val message: String) : BackupFeedback()
        data class Error(val message: String) : BackupFeedback()
    }

    private val _backupFeedbackEvents = MutableSharedFlow<BackupFeedback>(extraBufferCapacity = 8)
    val backupFeedbackEvents: SharedFlow<BackupFeedback> = _backupFeedbackEvents.asSharedFlow()

    fun saveDashboardColumns(columns: Int) {
        settingsRepository.saveDashboardColumns(columns)
    }

    fun saveThemeMode(mode: ThemeMode) {
        settingsRepository.saveThemeMode(mode)
    }

    fun saveColorPalette(palette: ColorPalette) {
        settingsRepository.saveColorPalette(palette)
    }

    fun saveDensity(density: Density) {
        settingsRepository.saveDensity(density)
    }

    fun suggestExportFileName(): String {
        val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return "ha-viewer-backup-$date$BACKUP_FILE_SUFFIX"
    }

    fun exportToUri(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = backupCodec.encode(settingsRepository.createBackupSnapshot())
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(bytes)
                    } ?: throw IOException("Could not open file for writing")
                }
            }
            result.fold(
                onSuccess = { postFeedback(BackupFeedback.Success("Backup exported successfully.")) },
                onFailure = {
                    postFeedback(
                        BackupFeedback.Error(
                            it.message?.takeIf { msg -> msg.isNotBlank() } ?: "Export failed"
                        )
                    )
                }
            )
        }
    }

    fun importFromUri(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IOException("Could not read backup file")
                    val snapshot = backupCodec.decode(bytes)
                    backupImporter.restore(snapshot)
                }
            }
            result.fold(
                onSuccess = { restoreResult ->
                    if (restoreResult.success) {
                        refreshSnapshots()
                        postFeedback(
                            BackupFeedback.Success(
                                restoreResult.message ?: "Backup imported successfully."
                            )
                        )
                    } else {
                        postFeedback(
                            BackupFeedback.Error(restoreResult.message ?: "Import failed")
                        )
                    }
                },
                onFailure = {
                    val message = when (it) {
                        is BackupParseException -> it.message ?: "Invalid backup file"
                        else -> it.message?.takeIf { msg -> msg.isNotBlank() } ?: "Import failed"
                    }
                    postFeedback(BackupFeedback.Error(message))
                }
            )
        }
    }

    fun saveInternalSnapshot(name: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    internalSnapshotStore.save(name, settingsRepository.createBackupSnapshot())
                }
            }
            result.fold(
                onSuccess = { meta ->
                    refreshSnapshots()
                    postFeedback(BackupFeedback.Success("Snapshot \"${meta.name}\" saved."))
                },
                onFailure = {
                    postFeedback(
                        BackupFeedback.Error(
                            it.message?.takeIf { msg -> msg.isNotBlank() } ?: "Could not save snapshot"
                        )
                    )
                }
            )
        }
    }

    fun restoreInternalSnapshot(id: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val snapshot = internalSnapshotStore.load(id)
                        ?: throw IllegalStateException("Snapshot not found")
                    backupImporter.restore(snapshot)
                }
            }
            result.fold(
                onSuccess = { restoreResult ->
                    if (restoreResult.success) {
                        postFeedback(
                            BackupFeedback.Success(
                                restoreResult.message ?: "Snapshot restored."
                            )
                        )
                    } else {
                        postFeedback(
                            BackupFeedback.Error(restoreResult.message ?: "Restore failed")
                        )
                    }
                },
                onFailure = {
                    postFeedback(
                        BackupFeedback.Error(
                            it.message?.takeIf { msg -> msg.isNotBlank() } ?: "Restore failed"
                        )
                    )
                }
            )
        }
    }

    fun deleteInternalSnapshot(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                internalSnapshotStore.delete(id)
            }
            refreshSnapshots()
            postFeedback(BackupFeedback.Success("Snapshot deleted."))
        }
    }

    private fun postFeedback(feedback: BackupFeedback) {
        _backupFeedbackEvents.tryEmit(feedback)
    }

    private fun refreshSnapshots() {
        _internalSnapshots.value = internalSnapshotStore.list()
    }
}
