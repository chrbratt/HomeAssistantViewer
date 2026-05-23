package se.inix.homeassistantviewer.domain.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class HistoryCsvEncoderTest {

    @Test
    fun `encode includes metadata header and detailed rows`() {
        val metadata = HistoryExportMetadata(
            exportedAt = Instant.parse("2026-05-23T12:00:00Z"),
            range = HistoryRange.Day,
            rangeStart = Instant.parse("2026-05-22T12:00:00Z"),
            rangeEnd = Instant.parse("2026-05-23T12:00:00Z")
        )
        val entity = HistoryEntityExport(
            connectionId = "c1",
            connectionName = "Home",
            entityId = "sensor.temp",
            displayName = "Temperature",
            series = HistorySeries(
                points = listOf(
                    HistoryPoint(
                        timestamp = Instant.parse("2026-05-23T10:00:00Z"),
                        value = 21.5,
                        rawState = "21.5"
                    )
                ),
                kind = SeriesKind.Numeric("°C")
            )
        )

        val csv = HistoryCsvEncoder.encode(metadata, listOf(entity)).decodeToString()

        assertTrue(csv.contains("# range_label,24 h"))
        assertTrue(csv.contains("timestamp_utc,connection_id"))
        assertTrue(csv.contains("2026-05-23T10:00:00Z,c1,Home,sensor.temp,Temperature,sensor,21.5,21.5,°C,numeric"))
    }

    @Test
    fun `encode filters rows outside selected range window`() {
        val metadata = HistoryExportMetadata(
            exportedAt = Instant.parse("2026-05-23T12:00:00Z"),
            range = HistoryRange.Day,
            rangeStart = Instant.parse("2026-05-22T12:00:00Z"),
            rangeEnd = Instant.parse("2026-05-23T12:00:00Z")
        )
        val entity = HistoryEntityExport(
            connectionId = "c1",
            connectionName = "Home",
            entityId = "sensor.temp",
            displayName = "Temperature",
            series = HistorySeries(
                points = listOf(
                    HistoryPoint(
                        timestamp = Instant.parse("2026-05-21T12:00:00Z"),
                        value = 10.0,
                        rawState = "10"
                    ),
                    HistoryPoint(
                        timestamp = Instant.parse("2026-05-23T10:00:00Z"),
                        value = 21.5,
                        rawState = "21.5"
                    )
                ),
                kind = SeriesKind.Numeric("°C")
            )
        )

        val csv = HistoryCsvEncoder.encode(metadata, listOf(entity)).decodeToString()

        assertEquals(1, HistoryCsvEncoder.rowCount(metadata, listOf(entity)))
        assertTrue(csv.contains("2026-05-23T10:00:00Z"))
        assertTrue(!csv.contains("2026-05-21T12:00:00Z"))
    }

    @Test
        assertEquals("plain", HistoryCsvEncoder.escape("plain"))
        assertEquals("\"say \"\"hi\"\"\"", HistoryCsvEncoder.escape("say \"hi\""))
        assertEquals("\"a,b\"", HistoryCsvEncoder.escape("a,b"))
    }

    @Test
    fun `suggestHistoryExportFileName sanitizes entity id`() {
        val name = suggestHistoryExportFileName(
            prefix = "sensor.living_room_temp",
            range = HistoryRange.Hour,
            exportedAt = Instant.parse("2026-05-23T12:00:00Z")
        )
        assertEquals("sensor.living_room_temp-1-h-2026-05-23.csv", name)
    }
}
