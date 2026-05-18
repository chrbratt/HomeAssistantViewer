package se.inix.homeassistantviewer.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.inix.homeassistantviewer.data.model.FavoriteItem
import se.inix.homeassistantviewer.data.model.HaConnection
import se.inix.homeassistantviewer.data.settings.ColorPalette
import se.inix.homeassistantviewer.data.settings.Density
import se.inix.homeassistantviewer.data.settings.ThemeMode

class BackupCodecTest {

    private val codec = BackupCodec()

    @Test
    fun `round-trip preserves connections tokens favourites with custom names and dividers`() {
        val original = AppBackupSnapshot(
            exportedAt = "2026-05-18T12:00:00Z",
            appVersion = "1.0.3",
            connections = listOf(
                HaConnection(
                    id = "conn-1",
                    name = "Home",
                    baseUrl = "http://192.168.1.10:8123",
                    token = "secret-token"
                )
            ),
            favorites = listOf(
                FavoriteBackupItem(
                    type = TYPE_ENTITY,
                    connectionId = "conn-1",
                    entityId = "light.kitchen",
                    customName = "Kök"
                ),
                FavoriteBackupItem(
                    type = TYPE_DIVIDER,
                    id = "div-1",
                    title = "Ute"
                ),
                FavoriteBackupItem(
                    type = TYPE_ENTITY,
                    connectionId = "conn-1",
                    entityId = "sensor.temp"
                )
            ),
            dashboard = DashboardBackupPrefs(
                columns = 2,
                themeMode = ThemeMode.DARK.name,
                colorPalette = ColorPalette.OCEAN.name,
                density = Density.COMPACT.name
            )
        )

        val decoded = codec.decode(codec.encode(original))

        assertEquals(original.connections, decoded.connections)
        assertEquals(original.favorites, decoded.favorites)
        assertEquals(original.dashboard, decoded.dashboard)
        assertEquals("Kök", decoded.favorites.first().customName)
    }

    @Test
    fun `favorite mappers preserve custom entity names and divider titles`() {
        val entity = FavoriteItem.Entity(
            connectionId = "c1",
            entityId = "light.hall",
            customName = "Hall lamp"
        )
        val divider = FavoriteItem.Divider(id = "d1", title = "Section A")

        val restoredEntity = entity.toBackupItem().toFavoriteItem() as FavoriteItem.Entity
        val restoredDivider = divider.toBackupItem().toFavoriteItem() as FavoriteItem.Divider

        assertEquals("Hall lamp", restoredEntity.customName)
        assertEquals("Section A", restoredDivider.title)
    }

    @Test
    fun `blank custom name is normalised to null on import`() {
        val item = FavoriteBackupItem(
            type = TYPE_ENTITY,
            connectionId = "c1",
            entityId = "light.a",
            customName = "   "
        )
        val fav = item.toFavoriteItem() as FavoriteItem.Entity
        assertEquals(null, fav.customName)
    }

    @Test
    fun `invalid JSON throws BackupParseException`() {
        val error = runCatching { codec.decode("{not json".encodeToByteArray()) }.exceptionOrNull()
        assertNotNull(error)
        assertTrue(error is BackupParseException)
    }
}
