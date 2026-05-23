package se.inix.homeassistantviewer.domain.comparison

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.inix.homeassistantviewer.domain.history.HistoryPoint
import se.inix.homeassistantviewer.domain.history.HistorySeries
import se.inix.homeassistantviewer.domain.history.SeriesKind
import java.time.Instant

class ComparisonChartGrouperTest {

    private val ts1 = Instant.parse("2026-01-01T00:00:00Z")
    private val ts2 = Instant.parse("2026-01-01T01:00:00Z")

    private fun numericSeries(unit: String?) = HistorySeries(
        points = listOf(
            HistoryPoint(ts1, 1.0, "1"),
            HistoryPoint(ts2, 2.0, "2")
        ),
        kind = SeriesKind.Numeric(unit)
    )

    private fun binarySeries() = HistorySeries(
        points = listOf(
            HistoryPoint(ts1, 1.0, "on"),
            HistoryPoint(ts2, 0.0, "off")
        ),
        kind = SeriesKind.Binary
    )

    private fun entry(
        id: String,
        unit: String?,
        series: HistorySeries
    ) = ComparisonSeriesEntry(
        connectionId = "c1",
        entityId = id,
        displayName = id,
        series = series,
        unit = unit,
        colorIndex = 0
    )

    @Test
    fun `single unit produces one chart group`() {
        val entries = listOf(
            entry("sensor.a", "°C", numericSeries("°C")),
            entry("sensor.b", "°C", numericSeries("°C"))
        )
        val result = ComparisonChartGrouper.group(entries, excluded = emptyList())
        assertEquals(1, result.chartGroups.size)
        assertEquals("°C", result.chartGroups.first().unitLabel)
        assertEquals(2, result.chartGroups.first().series.size)
    }

    @Test
    fun `two units produce separate chart groups`() {
        val entries = listOf(
            entry("sensor.temp", "°C", numericSeries("°C")),
            entry("sensor.hum", "%", numericSeries("%"))
        )
        val result = ComparisonChartGrouper.group(entries, excluded = emptyList())
        assertEquals(2, result.chartGroups.size)
        assertEquals(setOf("°C", "%"), result.chartGroups.map { it.unitLabel }.toSet())
    }

    @Test
    fun `three units produce separate chart groups`() {
        val entries = listOf(
            entry("sensor.a", "°C", numericSeries("°C")),
            entry("sensor.b", "%", numericSeries("%")),
            entry("sensor.c", "lx", numericSeries("lx"))
        )
        val result = ComparisonChartGrouper.group(entries, excluded = emptyList())
        assertEquals(3, result.chartGroups.size)
        assertEquals(setOf("°C", "%", "lx"), result.chartGroups.map { it.unitLabel }.toSet())
    }

    @Test
    fun `binary series are grouped separately from numeric`() {
        val entries = listOf(
            entry("switch.kitchen", null, binarySeries()),
            entry("sensor.temp", "°C", numericSeries("°C"))
        )
        val result = ComparisonChartGrouper.group(entries, excluded = emptyList())
        assertEquals(1, result.chartGroups.size)
        assertEquals(1, result.binarySeries.size)
        assertTrue(result.excluded.isEmpty())
    }

    @Test
    fun `binary-only selection produces binary series`() {
        val entries = listOf(
            entry("switch.kitchen", null, binarySeries()),
            entry("switch.bedroom", null, binarySeries())
        )
        val result = ComparisonChartGrouper.group(entries, excluded = emptyList())
        assertTrue(result.chartGroups.isEmpty())
        assertEquals(2, result.binarySeries.size)
        assertTrue(result.excluded.isEmpty())
    }
}
