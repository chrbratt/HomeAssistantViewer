package se.inix.homeassistantviewer.domain.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeriesClassifierTest {

    @Test
    fun `binary domains classify as Binary`() {
        listOf("switch", "light", "binary_sensor", "input_boolean", "lock", "fan").forEach {
            assertEquals(SeriesKind.Binary, SeriesClassifier.classify(it, null))
        }
    }

    @Test
    fun `sensor with unit classifies as Numeric carrying the unit`() {
        val kind = SeriesClassifier.classify("sensor", "°C")
        assertEquals(SeriesKind.Numeric("°C"), kind)
    }

    @Test
    fun `sensor without unit classifies as Discrete`() {
        assertEquals(SeriesKind.Discrete, SeriesClassifier.classify("sensor", null))
    }

    @Test
    fun `unknown domain without unit classifies as Discrete`() {
        assertEquals(SeriesKind.Discrete, SeriesClassifier.classify("weather", null))
    }

    @Test
    fun `binary projection maps common on-states to one and off-states to zero`() {
        val kind = SeriesKind.Binary
        listOf("on", "open", "locked", "playing", "home", "true").forEach {
            assertEquals(1.0, SeriesClassifier.project(kind, it))
        }
        listOf("off", "closed", "unlocked", "idle", "not_home", "false").forEach {
            assertEquals(0.0, SeriesClassifier.project(kind, it))
        }
    }

    @Test
    fun `binary projection returns null for unknown state`() {
        assertNull(SeriesClassifier.project(SeriesKind.Binary, "unavailable"))
        assertNull(SeriesClassifier.project(SeriesKind.Binary, "weird"))
    }

    @Test
    fun `numeric projection parses doubles and rejects garbage`() {
        val kind = SeriesKind.Numeric("°C")
        assertEquals(21.5, SeriesClassifier.project(kind, "21.5"))
        assertEquals(-3.0, SeriesClassifier.project(kind, "-3"))
        assertNull(SeriesClassifier.project(kind, "unknown"))
    }

    @Test
    fun `discrete projection is always null`() {
        assertNull(SeriesClassifier.project(SeriesKind.Discrete, "sunny"))
    }
}
