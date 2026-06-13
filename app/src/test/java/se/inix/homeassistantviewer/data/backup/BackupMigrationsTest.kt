package se.inix.homeassistantviewer.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import se.inix.homeassistantviewer.data.model.HaConnection
import se.inix.homeassistantviewer.data.settings.ColorPalette
import se.inix.homeassistantviewer.data.settings.Density
import se.inix.homeassistantviewer.data.settings.TextContrast
import se.inix.homeassistantviewer.data.settings.ThemeMode

class BackupMigrationsTest {

    @Test
    fun `v1 backup gains current version, empty comparison list and default text contrast`() {
        val v1 = AppBackupSnapshot(
            formatVersion = 1,
            exportedAt = "2026-01-01T00:00:00Z",
            appVersion = "1.0.0",
            connections = emptyList(),
            favorites = emptyList(),
            dashboard = DashboardBackupPrefs(
                columns = 2,
                themeMode = ThemeMode.DARK.name,
                colorPalette = ColorPalette.OCEAN.name,
                density = Density.COMFORTABLE.name
            ),
            comparisonSelection = null
        )

        val migrated = BackupMigrations.migrateToCurrent(v1)

        assertEquals(AppBackupSnapshot.CURRENT_FORMAT_VERSION, migrated.formatVersion)
        assertEquals(emptyList<String>(), migrated.comparisonSelection)
        assertEquals(TextContrast.BALANCED.name, migrated.dashboard.textContrast)
    }

    @Test
    fun `unknown preference values are coerced to safe defaults`() {
        val odd = currentSnapshot().copy(
            dashboard = DashboardBackupPrefs(
                columns = 9,
                themeMode = "ULTRA_DARK",
                colorPalette = "NEON_PINK",
                density = "WIDE",
                textContrast = "GLOWING"
            )
        )

        val d = BackupMigrations.migrateToCurrent(odd).dashboard

        assertEquals(3, d.columns)
        assertEquals(ThemeMode.SYSTEM.name, d.themeMode)
        assertEquals(ColorPalette.DYNAMIC.name, d.colorPalette)
        assertEquals(Density.COMFORTABLE.name, d.density)
        assertEquals(TextContrast.BALANCED.name, d.textContrast)
    }

    @Test
    fun `known values survive migration unchanged`() {
        val snapshot = currentSnapshot().copy(
            dashboard = DashboardBackupPrefs(
                columns = 1,
                themeMode = ThemeMode.LIGHT.name,
                colorPalette = ColorPalette.AURORA.name,
                density = Density.COMPACT.name,
                textContrast = TextContrast.SOFT.name
            )
        )

        assertEquals(snapshot.dashboard, BackupMigrations.migrateToCurrent(snapshot).dashboard)
    }

    @Test
    fun `migration is idempotent`() {
        val once = BackupMigrations.migrateToCurrent(currentSnapshot().copy(formatVersion = 1))
        val twice = BackupMigrations.migrateToCurrent(once)
        assertEquals(once, twice)
    }

    @Test
    fun `a newer-than-known backup keeps its version so the importer can still reject it`() {
        val future = currentSnapshot().copy(formatVersion = 99)
        assertEquals(99, BackupMigrations.migrateToCurrent(future).formatVersion)
    }

    @Test
    fun `an old backup with an unknown palette now passes pre-flight validation`() {
        val legacy = AppBackupSnapshot(
            formatVersion = 1,
            exportedAt = "2026-01-01T00:00:00Z",
            appVersion = "1.0.0",
            connections = listOf(
                HaConnection("conn-1", "Home", "http://192.168.1.1:8123", "token")
            ),
            favorites = emptyList(),
            dashboard = DashboardBackupPrefs(
                columns = 2,
                themeMode = ThemeMode.SYSTEM.name,
                colorPalette = "RETIRED_PALETTE",
                density = Density.COMFORTABLE.name
            ),
            comparisonSelection = null
        )

        val migrated = BackupMigrations.migrateToCurrent(legacy)

        assertNull(validateForRestore(migrated))
    }

    private fun currentSnapshot() = AppBackupSnapshot(
        exportedAt = "2026-01-01T00:00:00Z",
        appVersion = "1.0.0",
        connections = emptyList(),
        favorites = emptyList(),
        dashboard = DashboardBackupPrefs(
            columns = 2,
            themeMode = ThemeMode.SYSTEM.name,
            colorPalette = ColorPalette.DYNAMIC.name,
            density = Density.COMFORTABLE.name
        )
    )
}
