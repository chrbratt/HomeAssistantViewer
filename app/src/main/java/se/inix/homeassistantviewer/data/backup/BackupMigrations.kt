package se.inix.homeassistantviewer.data.backup

import se.inix.homeassistantviewer.data.settings.ColorPalette
import se.inix.homeassistantviewer.data.settings.Density
import se.inix.homeassistantviewer.data.settings.TextContrast
import se.inix.homeassistantviewer.data.settings.ThemeMode

/**
 * Upgrades a decoded [AppBackupSnapshot] from any older on-disk format to
 * [AppBackupSnapshot.CURRENT_FORMAT_VERSION].
 *
 * Two reasons this exists instead of relying purely on Moshi defaults:
 *
 *  1. Older backups (and internal snapshots) genuinely predate fields like
 *     `comparisonSelection` (added in v2) and `dashboard.textContrast`
 *     (added in v3). Migration fills those in explicitly and stamps the
 *     version, so a restored old file behaves exactly like a freshly made
 *     one.
 *  2. Backward compatibility: a value that no longer maps to a known enum
 *     (e.g. a palette removed in a later build, or a typo'd theme mode) is
 *     coerced to a safe default rather than failing the whole restore. The
 *     user keeps their connections and favourites; only the unknown UI
 *     preference falls back.
 *
 * Pure and idempotent — running it twice yields the same result — so it is
 * safe to call on every restore regardless of the source version.
 */
object BackupMigrations {

    fun migrateToCurrent(snapshot: AppBackupSnapshot): AppBackupSnapshot {
        // Never downgrade a newer-than-known file here; the importer's
        // pre-flight still rejects it. Older/equal files advance to current.
        val targetVersion = maxOf(snapshot.formatVersion, AppBackupSnapshot.CURRENT_FORMAT_VERSION)
        return snapshot.copy(
            formatVersion = targetVersion,
            comparisonSelection = snapshot.comparisonSelection ?: emptyList(),
            dashboard = snapshot.dashboard.coerceToKnownValues(),
        )
    }

    private fun DashboardBackupPrefs.coerceToKnownValues(): DashboardBackupPrefs = copy(
        columns = columns.coerceIn(1, 3),
        themeMode = themeMode.orDefault(ThemeMode.SYSTEM.name) { ThemeMode.valueOf(it) },
        colorPalette = colorPalette.orDefault(ColorPalette.DYNAMIC.name) { ColorPalette.valueOf(it) },
        density = density.orDefault(Density.COMFORTABLE.name) { Density.valueOf(it) },
        textContrast = textContrast.orDefault(TextContrast.BALANCED.name) { TextContrast.valueOf(it) },
    )

    /** Returns [this] if [parse] accepts it, otherwise [default]. */
    private inline fun String.orDefault(default: String, parse: (String) -> Any): String =
        if (runCatching { parse(this) }.isSuccess) this else default
}
