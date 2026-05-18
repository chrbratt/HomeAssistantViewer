package se.inix.homeassistantviewer.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.inix.homeassistantviewer.data.model.HaConnection

/**
 * Tests backup restore pre-flight validation without touching [SettingsRepository].
 */
class BackupImporterValidationTest {

    @Test
    fun `validateForRestore rejects duplicate connection ids`() {
        val snapshot = snapshotWithConnections(
            HaConnection("same-id", "A", "http://192.168.1.1:8123", "token-a"),
            HaConnection("same-id", "B", "http://192.168.1.2:8123", "token-b")
        )
        val result = validateForRestore(snapshot)
        assertNotNull(result)
        assertFalse(result!!.success)
        assertTrue(result.message!!.contains("duplicate", ignoreCase = true))
    }

    @Test
    fun `validateForRestore rejects blank connection id`() {
        val snapshot = snapshotWithConnections(
            HaConnection("", "Home", "http://192.168.1.1:8123", "token")
        )
        val result = validateForRestore(snapshot)
        assertNotNull(result)
        assertFalse(result!!.success)
    }

    @Test
    fun `validateForRestore rejects entity favourites when no valid connections`() {
        val snapshot = AppBackupSnapshot(
            exportedAt = "2026-01-01T00:00:00Z",
            appVersion = "1.0.0",
            connections = emptyList(),
            favorites = listOf(
                FavoriteBackupItem(
                    type = TYPE_ENTITY,
                    connectionId = "missing",
                    entityId = "light.kitchen"
                )
            ),
            dashboard = validDashboard()
        )
        val result = validateForRestore(snapshot)
        assertNotNull(result)
        assertFalse(result!!.success)
        assertTrue(result.message!!.contains("favourites", ignoreCase = true))
    }

    @Test
    fun `validateForRestore accepts valid snapshot`() {
        val snapshot = snapshotWithConnections(
            HaConnection("conn-1", "Home", "http://192.168.1.1:8123", "token")
        )
        assertNull(validateForRestore(snapshot))
    }

    private fun snapshotWithConnections(vararg connections: HaConnection) = AppBackupSnapshot(
        exportedAt = "2026-01-01T00:00:00Z",
        appVersion = "1.0.0",
        connections = connections.toList(),
        favorites = emptyList(),
        dashboard = validDashboard()
    )

    private fun validDashboard() = DashboardBackupPrefs(
        columns = 2,
        themeMode = "SYSTEM",
        colorPalette = "DYNAMIC",
        density = "COMFORTABLE"
    )
}
