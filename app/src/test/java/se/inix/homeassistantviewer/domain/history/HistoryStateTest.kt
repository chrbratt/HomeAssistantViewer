package se.inix.homeassistantviewer.domain.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.inix.homeassistantviewer.data.model.HaHistoryRow
import java.time.Instant

class HistoryStateTest {

    private fun pt(epoch: Long, state: String) = HistoryPoint(
        timestamp = Instant.ofEpochSecond(epoch),
        value = null,
        rawState = state
    )

    @Test
    fun `isPlottableHistoryState rejects blank and HA sentinel states`() {
        assertFalse(isPlottableHistoryState(""))
        assertFalse(isPlottableHistoryState("unavailable"))
        assertFalse(isPlottableHistoryState("UNKNOWN"))
        assertTrue(isPlottableHistoryState("sunny"))
    }

    @Test
    fun `computeStateIntervals emits one interval per held state`() {
        val intervals = computeStateIntervals(
            listOf(pt(100, "sunny"), pt(200, "cloudy"), pt(300, "rainy")),
            windowEnd = 400L
        )
        assertEquals(
            listOf(
                StateInterval(100, 200, "sunny"),
                StateInterval(200, 300, "cloudy"),
                StateInterval(300, 400, "rainy")
            ),
            intervals
        )
    }

    @Test
    fun `computeStateIntervals skips unplottable states`() {
        val intervals = computeStateIntervals(
            listOf(pt(100, "sunny"), pt(200, "unavailable"), pt(300, "rainy")),
            windowEnd = 400L
        )
        assertEquals(
            listOf(
                StateInterval(100, 200, "sunny"),
                StateInterval(300, 400, "rainy")
            ),
            intervals
        )
    }

    @Test
    fun `orderedDistinctStates preserves first-seen order`() {
        val points = listOf(
            pt(1, "rainy"),
            pt(2, "sunny"),
            pt(3, "rainy"),
            pt(4, "cloudy")
        )
        assertEquals(listOf("rainy", "sunny", "cloudy"), orderedDistinctStates(points))
    }
}

class CategoricalSeriesBuilderTest {

    @Test
    fun `weather domain produces Categorical series with state list`() {
        val rows = listOf(
            HaHistoryRow(state = "sunny", lastChanged = "2026-01-01T00:00:00Z"),
            HaHistoryRow(state = "cloudy", lastChanged = "2026-01-01T01:00:00Z"),
            HaHistoryRow(state = "rainy", lastChanged = "2026-01-01T02:00:00Z")
        )
        val series = HistorySeriesBuilder.build(rows, domain = "weather", unitOfMeasurement = null)
        assertTrue(series.kind is SeriesKind.Categorical)
        val kind = series.kind as SeriesKind.Categorical
        assertEquals(listOf("sunny", "cloudy", "rainy"), kind.states)
        assertTrue(series.hasPlottableData())
    }

    @Test
    fun `two-state home not_home history upgrades to Binary`() {
        val rows = listOf(
            HaHistoryRow(state = "home", lastChanged = "2026-01-01T00:00:00Z"),
            HaHistoryRow(state = "not_home", lastChanged = "2026-01-01T01:00:00Z")
        )
        val series = HistorySeriesBuilder.build(rows, domain = "device_tracker", unitOfMeasurement = null)
        assertEquals(SeriesKind.Binary, series.kind)
        assertEquals(listOf(1.0, 0.0), series.points.map { it.value })
    }

    @Test
    fun `sensor without unit becomes Numeric when all states are numbers`() {
        val rows = listOf(
            HaHistoryRow(state = "20.5", lastChanged = "2026-01-01T00:00:00Z"),
            HaHistoryRow(state = "21.0", lastChanged = "2026-01-01T01:00:00Z")
        )
        val series = HistorySeriesBuilder.build(rows, domain = "sensor", unitOfMeasurement = null)
        assertTrue(series.kind is SeriesKind.Numeric)
        assertEquals(listOf(20.5, 21.0), series.points.map { it.value })
    }

    @Test
    fun `input_select with on and off stays Categorical not Binary`() {
        val rows = listOf(
            HaHistoryRow(state = "on", lastChanged = "2026-01-01T00:00:00Z"),
            HaHistoryRow(state = "off", lastChanged = "2026-01-01T01:00:00Z")
        )
        val series = HistorySeriesBuilder.build(rows, domain = "input_select", unitOfMeasurement = null)
        assertTrue(series.kind is SeriesKind.Categorical)
    }
}
