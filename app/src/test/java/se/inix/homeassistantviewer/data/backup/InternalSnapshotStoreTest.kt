package se.inix.homeassistantviewer.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class InternalSnapshotStoreTest {

    private fun tempDir(): File {
        val dir = File.createTempFile("snapshot-test-", null)
        check(dir.delete())
        return dir
    }

    private fun minimalSnapshot() = AppBackupSnapshot(
        exportedAt = "2026-05-18T12:00:00Z",
        appVersion = "1.0.4",
        connections = emptyList(),
        favorites = emptyList(),
        dashboard = DashboardBackupPrefs(
            columns = 2,
            themeMode = "SYSTEM",
            colorPalette = "DYNAMIC",
            density = "COMFORTABLE"
        )
    )

    @Test
    fun `save appears in list immediately`() {
        val store = InternalSnapshotStore.forTesting(tempDir())

        store.save("Production", minimalSnapshot())

        val listed = store.list()
        assertEquals(1, listed.size)
        assertEquals("Production", listed[0].name)
    }

    @Test
    fun `save persists snapshot file and index across store reopen`() {
        val dir = tempDir()
        InternalSnapshotStore.forTesting(dir).save("My layout", minimalSnapshot())

        val reopened = InternalSnapshotStore.forTesting(dir)
        val afterReopen = reopened.list()
        assertEquals(1, afterReopen.size)
        assertEquals("My layout", afterReopen[0].name)
        assertNotNull(reopened.load(afterReopen[0].id))
    }

    @Test
    fun `list returns newest first`() {
        val store = InternalSnapshotStore.forTesting(tempDir())
        store.save("Older", minimalSnapshot())
        Thread.sleep(1_100)
        store.save("Newer", minimalSnapshot())

        assertEquals(listOf("Newer", "Older"), store.list().map { it.name })
    }

    @Test
    fun `delete removes snapshot from list`() {
        val store = InternalSnapshotStore.forTesting(tempDir())
        val meta = store.save("To delete", minimalSnapshot())

        store.delete(meta.id)

        assertTrue(store.list().isEmpty())
    }
}
