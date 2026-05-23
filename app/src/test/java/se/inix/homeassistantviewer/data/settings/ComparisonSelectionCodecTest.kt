package se.inix.homeassistantviewer.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.inix.homeassistantviewer.data.model.ComparisonEntity

class ComparisonSelectionCodecTest {

    private val entity1 = ComparisonEntity("c1", "sensor.temp")
    private val entity2 = ComparisonEntity("c2", "sensor.humidity")

    @Test
    fun `round-trips empty selection`() {
        val raw = ComparisonSelectionCodec.serialize(emptySet())
        assertEquals("", raw)
        assertTrue(ComparisonSelectionCodec.deserialize(raw).isEmpty())
    }

    @Test
    fun `round-trips multiple entities`() {
        val input = setOf(entity1, entity2)
        val raw = ComparisonSelectionCodec.serialize(input)
        assertEquals(input, ComparisonSelectionCodec.deserialize(raw))
    }

    @Test
    fun `ignores malformed tokens`() {
        val raw = "e:c1/sensor.a,bad-token,e:c2/sensor.b"
        assertEquals(
            setOf(
                ComparisonEntity("c1", "sensor.a"),
                ComparisonEntity("c2", "sensor.b")
            ),
            ComparisonSelectionCodec.deserialize(raw)
        )
    }
}
