package se.inix.homeassistantviewer.data.backup

import se.inix.homeassistantviewer.data.events.AppEvents
import se.inix.homeassistantviewer.data.model.FavoriteItem
import se.inix.homeassistantviewer.data.model.HaConnection
import se.inix.homeassistantviewer.data.settings.ColorPalette
import se.inix.homeassistantviewer.data.settings.Density
import se.inix.homeassistantviewer.data.settings.SettingsRepository
import se.inix.homeassistantviewer.data.settings.ThemeMode
import se.inix.homeassistantviewer.data.settings.UrlNormaliser

/**
 * Validates a decoded [AppBackupSnapshot] and applies it through
 * [SettingsRepository.restoreBackupSnapshot].
 */
class BackupImporter(
    private val settingsRepository: SettingsRepository,
    private val appEvents: AppEvents
) {

    data class RestoreResult(
        val success: Boolean,
        val skippedFavorites: Int = 0,
        val message: String? = null
    )

    fun restore(snapshot: AppBackupSnapshot): RestoreResult {
        validateForRestore(snapshot)?.let { return it }

        val connections = snapshot.connections.mapNotNull { validateConnection(it) }
        val connectionIds = connections.map { it.id }.toSet()
        var skipped = 0
        val favorites = snapshot.favorites.mapNotNull { item ->
            val fav = item.toFavoriteItem() ?: run {
                skipped++
                return@mapNotNull null
            }
            if (fav is FavoriteItem.Entity && fav.connectionId !in connectionIds) {
                skipped++
                null
            } else {
                fav
            }
        }

        val dashboard = parseDashboard(snapshot.dashboard)
            ?: return RestoreResult(success = false, message = "Invalid dashboard settings in backup.")

        settingsRepository.restoreBackupSnapshot(
            connections = connections,
            favorites = favorites,
            dashboard = dashboard
        )
        appEvents.notifyConfigurationRestored()

        val message = when {
            skipped == 0 -> null
            skipped == 1 -> "Restored. 1 favourite was skipped (missing connection)."
            else -> "Restored. $skipped favourites were skipped (missing connections)."
        }
        return RestoreResult(success = true, skippedFavorites = skipped, message = message)
    }

    private fun validateConnection(raw: HaConnection): HaConnection? =
        validateConnectionForBackup(raw)

    private fun parseDashboard(raw: DashboardBackupPrefs): DashboardBackupPrefs? =
        parseDashboardForBackup(raw)
}

/** Pre-flight validation; returns an error [BackupImporter.RestoreResult] or null if OK. */
internal fun validateForRestore(snapshot: AppBackupSnapshot): BackupImporter.RestoreResult? {
    if (snapshot.formatVersion > AppBackupSnapshot.CURRENT_FORMAT_VERSION) {
        return BackupImporter.RestoreResult(
            success = false,
            message = "Backup was created by a newer app version. Update HA Viewer first."
        )
    }

    val connections = snapshot.connections.mapNotNull { validateConnectionForBackup(it) }
    if (connections.isEmpty() && snapshot.connections.isNotEmpty()) {
        return BackupImporter.RestoreResult(
            success = false,
            message = "No valid connections found in the backup file."
        )
    }

    if (connections.map { it.id }.toSet().size != connections.size) {
        return BackupImporter.RestoreResult(
            success = false,
            message = "Backup contains duplicate connection IDs."
        )
    }

    val hasEntityFavorites = snapshot.favorites.any { it.type == TYPE_ENTITY }
    if (connections.isEmpty() && hasEntityFavorites) {
        return BackupImporter.RestoreResult(
            success = false,
            message = "Backup has entity favourites but no valid connections."
        )
    }

    if (parseDashboardForBackup(snapshot.dashboard) == null) {
        return BackupImporter.RestoreResult(
            success = false,
            message = "Invalid dashboard settings in backup."
        )
    }

    return null
}

internal fun validateConnectionForBackup(raw: HaConnection): HaConnection? {
    if (raw.id.isBlank()) return null
    val url = UrlNormaliser.normalise(raw.baseUrl) ?: return null
    if (raw.token.isBlank()) return null
    return raw.copy(
        name = raw.name.ifBlank { "Home Assistant" },
        baseUrl = url,
        token = raw.token.trim()
    )
}

internal fun parseDashboardForBackup(raw: DashboardBackupPrefs): DashboardBackupPrefs? {
    if (raw.columns !in 1..3) return null
    if (runCatching { ThemeMode.valueOf(raw.themeMode) }.isFailure) return null
    if (runCatching { ColorPalette.valueOf(raw.colorPalette) }.isFailure) return null
    if (runCatching { Density.valueOf(raw.density) }.isFailure) return null
    return raw
}
