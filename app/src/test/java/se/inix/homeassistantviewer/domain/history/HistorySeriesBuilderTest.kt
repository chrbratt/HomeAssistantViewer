package se.inix.homeassistantviewer.domain.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.inix.homeassistantviewer.data.model.HaHistoryRow

class HistorySeriesBuilderTest {

    @Test
    fun `binary domain produces Binary series with 0 and 1 values`() {
        val rows = listOf(
            HaHistoryRow(state = "off", lastChanged = "2026-01-01T00:00:00Z"),
            HaHistoryRow(state = "on", lastChanged = "2026-01-01T01:00:00Z"),
            HaHistoryRow(state = "off", lastChanged = "2026-01-01T02:00:00Z")
        )
        val series = HistorySeriesBuilder.build(rows, domain = "switch", unitOfMeasurement = null)
        assertEquals(SeriesKind.Binary, series.kind)
        assertEquals(listOf(0.0, 1.0, 0.0), series.points.map { it.value })
    }

    @Test
    fun `numeric sensor with unit produces Numeric series carrying the unit`() {
        val rows = listOf(
            HaHistoryRow(state = "20.5", lastChanged = "2026-01-01T00:00:00Z"),
            HaHistoryRow(state = "21.0", lastChanged = "2026-01-01T01:00:00Z")
        )
        val series = HistorySeriesBuilder.build(rows, domain = "sensor", unitOfMeasurement = "°C")
        assertEquals(SeriesKind.Numeric("°C"), series.kind)
        assertEquals(listOf(20.5, 21.0), series.points.map { it.value })
    }

    @Test
    fun `unavailable rows in binary series end up as null and are kept as gaps`() {
        val rows = listOf(
            HaHistoryRow(state = "on", lastChanged = "2026-01-01T00:00:00Z"),
            HaHistoryRow(state = "unavailable", lastChanged = "2026-01-01T01:00:00Z"),
            HaHistoryRow(state = "off", lastChanged = "2026-01-01T02:00:00Z")
        )
        val series = HistorySeriesBuilder.build(rows, domain = "switch", unitOfMeasurement = null)
        assertEquals(listOf(1.0, null, 0.0), series.points.map { it.value })
    }

    @Test
    fun `malformed timestamps are dropped without crashing the build`() {
        val rows = listOf(
            HaHistoryRow(state = "1", lastChanged = "not-a-date"),
            HaHistoryRow(state = "2", lastChanged = "2026-01-01T00:00:00Z")
        )
        val series = HistorySeriesBuilder.build(rows, domain = "sensor", unitOfMeasurement = "W")
        assertEquals(1, series.points.size)
        assertEquals(2.0, series.points.single().value)
    }

    @Test
    fun `JSON-null state in a binary switch series becomes a gap, not a crash`() {
        // Mirrors the real HA payload that triggered the
        // "Non-Null value 'state' was null at $[0][5].state" Moshi error:
        // a switch row where recorder logged the state value as JSON null.
        val rows = listOf(
            HaHistoryRow(state = "on", lastChanged = "2026-01-01T00:00:00Z"),
            HaHistoryRow(state = null, lastChanged = "2026-01-01T00:01:00Z"),
            HaHistoryRow(state = "off", lastChanged = "2026-01-01T00:02:00Z")
        )
        val series = HistorySeriesBuilder.build(rows, domain = "switch", unitOfMeasurement = null)
        assertEquals("null state row must survive as a gap point",
            listOf(1.0, null, 0.0), series.points.map { it.value })
        assertEquals("rawState for a null state should be the empty string, not 'null'",
            "", series.points[1].rawState)
    }

    @Test
    fun `row without a timestamp is dropped — there's no X coordinate to plot`() {
        val rows = listOf(
            HaHistoryRow(state = "on", lastChanged = null),
            HaHistoryRow(state = "off", lastChanged = "2026-01-01T00:00:00Z")
        )
        val series = HistorySeriesBuilder.build(rows, domain = "switch", unitOfMeasurement = null)
        assertEquals(1, series.points.size)
        assertEquals(0.0, series.points.single().value)
    }

    @Test
    fun `output is chronologically sorted regardless of input order`() {
        val rows = listOf(
            HaHistoryRow(state = "3", lastChanged = "2026-01-01T03:00:00Z"),
            HaHistoryRow(state = "1", lastChanged = "2026-01-01T01:00:00Z"),
            HaHistoryRow(state = "2", lastChanged = "2026-01-01T02:00:00Z")
        )
        val series = HistorySeriesBuilder.build(rows, domain = "sensor", unitOfMeasurement = "W")
        val timestamps = series.points.map { it.timestamp.toEpochMilli() }
        assertTrue(timestamps == timestamps.sorted())
    }
}
