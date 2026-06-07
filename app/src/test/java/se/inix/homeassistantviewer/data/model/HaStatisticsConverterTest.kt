package se.inix.homeassistantviewer.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HaStatisticsConverterTest {

    @Test
    fun `maps mean to state and start ms to ISO timestamp`() {
        val rows = HaStatisticsConverter.toHistoryRows(
            listOf(HaStatisticsPoint(startMs = 1_752_012_000_000, mean = 21.21))
        )
        assertEquals(1, rows.size)
        assertEquals("21.21", rows[0].state)
        assertEquals("2025-07-08T22:00:00Z", rows[0].lastChanged)
    }

    @Test
    fun `formats whole numbers without trailing decimal`() {
        val rows = HaStatisticsConverter.toHistoryRows(
            listOf(HaStatisticsPoint(startMs = 0, mean = 20.0))
        )
        assertEquals("20", rows[0].state)
    }

    @Test
    fun `drops buckets without a mean`() {
        val rows = HaStatisticsConverter.toHistoryRows(
            listOf(
                HaStatisticsPoint(startMs = 0, mean = null),
                HaStatisticsPoint(startMs = 3_600_000, mean = 19.5)
            )
        )
        assertEquals(1, rows.size)
        assertEquals("19.5", rows[0].state)
    }

    @Test
    fun `empty input yields empty output`() {
        assertTrue(HaStatisticsConverter.toHistoryRows(emptyList()).isEmpty())
    }
}
