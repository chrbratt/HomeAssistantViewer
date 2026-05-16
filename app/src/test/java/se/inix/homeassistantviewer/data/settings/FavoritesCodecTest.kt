package se.inix.homeassistantviewer.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import se.inix.homeassistantviewer.data.model.FavoriteItem

/**
 * Tests for [FavoritesCodec] — the serialization layer for the favourites
 * DataStore preference. These tests pin two things that, if broken, would
 * cause silent data loss for existing users:
 *
 *  1. Round-tripping is lossless for every flavour of [FavoriteItem]
 *     (entity, named entity, divider, titled divider), including special
 *     characters in names / titles.
 *  2. Legacy payloads (written before custom names / titles existed)
 *     still parse into the right shape.
 */
class FavoritesCodecTest {

    private val entity = FavoriteItem.Entity(connectionId = "c1", entityId = "light.kitchen")
    private val namedEntity = entity.copy(customName = "Kök")
    private val divider = FavoriteItem.Divider(id = "d-1")
    private val titledDivider = divider.copy(title = "Vardagsrum")

    // --- Round-trip --------------------------------------------------------

    @Test
    fun `round-trips an empty list`() {
        val raw = FavoritesCodec.serialize(emptyList())
        assertEquals("", raw)
        assertEquals(emptyList<FavoriteItem>(), FavoritesCodec.deserialize(raw))
    }

    @Test
    fun `round-trips a list containing every variant`() {
        val input = listOf(entity, namedEntity, divider, titledDivider)
        val raw = FavoritesCodec.serialize(input)
        assertEquals(input, FavoritesCodec.deserialize(raw))
    }

    @Test
    fun `custom names survive reserved characters (comma, pipe, percent)`() {
        // These three are exactly the characters that would break either
        // the outer comma-split or the inner pipe-split if we weren't
        // URL-encoding the variable text fields.
        val tricky = entity.copy(customName = "Hall, A|B 100% sure")
        val raw = FavoritesCodec.serialize(listOf(tricky))
        assertEquals(listOf(tricky), FavoritesCodec.deserialize(raw))
    }

    @Test
    fun `divider titles survive newline and emoji`() {
        // Anything Compose can render should round-trip. Emoji exercise
        // multi-byte UTF-8 through URL encoding.
        val tricky = divider.copy(title = "Hall\n🏠 Section")
        val raw = FavoritesCodec.serialize(listOf(tricky))
        assertEquals(listOf(tricky), FavoritesCodec.deserialize(raw))
    }

    // --- Empty / blank custom names --------------------------------------

    @Test
    fun `blank custom name is omitted from serialized output`() {
        // A blank name is semantically "no override" — we shouldn't emit
        // a trailing empty pipe-segment that future readers would have to
        // special-case.
        val raw = FavoritesCodec.serialize(listOf(entity.copy(customName = "   ")))
        assertEquals("e:c1|light.kitchen", raw)
    }

    @Test
    fun `blank divider title is omitted from serialized output`() {
        val raw = FavoritesCodec.serialize(listOf(divider.copy(title = "")))
        assertEquals("d:d-1", raw)
    }

    // --- Legacy compatibility --------------------------------------------

    @Test
    fun `legacy entity-only payload still parses`() {
        // Pre-v? payload — entity tokens were just `connId|entityId`.
        val parsed = FavoritesCodec.deserialize("c1|light.kitchen,c2|sensor.temp")
        assertEquals(
            listOf(
                FavoriteItem.Entity("c1", "light.kitchen"),
                FavoriteItem.Entity("c2", "sensor.temp")
            ),
            parsed
        )
    }

    @Test
    fun `legacy divider (bare UUID) still parses`() {
        // Pre-titles payload — dividers were bare UUIDs.
        val parsed = FavoritesCodec.deserialize("d-1")
        assertEquals(listOf(FavoriteItem.Divider(id = "d-1")), parsed)
    }

    @Test
    fun `mixed legacy and new tokens parse together`() {
        // Realistic upgrade scenario: half the list is from a previous
        // version, half has been re-saved with the new format.
        val parsed = FavoritesCodec.deserialize(
            "c1|light.kitchen,d-1,e:c2|sensor.temp|Living%20room,d:d-2|Hall"
        )
        assertEquals(
            listOf(
                FavoriteItem.Entity("c1", "light.kitchen"),
                FavoriteItem.Divider("d-1"),
                FavoriteItem.Entity("c2", "sensor.temp", customName = "Living room"),
                FavoriteItem.Divider("d-2", title = "Hall")
            ),
            parsed
        )
    }

    // --- Malformed input is silently dropped, not crashing ---------------

    @Test
    fun `empty tokens between commas are skipped`() {
        val parsed = FavoritesCodec.deserialize(",,c1|x.y,,,")
        assertEquals(listOf(FavoriteItem.Entity("c1", "x.y")), parsed)
    }

    @Test
    fun `malformed entity token (missing entityId) is dropped`() {
        val parsed = FavoritesCodec.deserialize("e:c1|,c2|light.kitchen")
        assertEquals(listOf(FavoriteItem.Entity("c2", "light.kitchen")), parsed)
    }

    @Test
    fun `malformed URL-encoded name decodes to null and falls back gracefully`() {
        // Invalid percent-escape (`%ZZ`) → decoder returns null → entity
        // is parsed without a custom name rather than crashing.
        val parsed = FavoritesCodec.deserialize("e:c1|light.kitchen|%ZZ")
        assertEquals(1, parsed.size)
        val entity = parsed.first() as FavoriteItem.Entity
        assertEquals("c1", entity.connectionId)
        assertEquals("light.kitchen", entity.entityId)
        assertNull(entity.customName)
    }
}
