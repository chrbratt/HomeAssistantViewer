package se.inix.homeassistantviewer.domain.history

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.inix.homeassistantviewer.data.model.HaHistoryRow

/**
 * Regression test that pins the end-to-end history pipeline against a real
 * Home Assistant response (Moshi → builder → projected chart points).
 *
 * Guards two subtle behaviours discovered while debugging the
 * "graph is just a flat line" report:
 *
 *  1. HA history rows return their timestamp with `+00:00` offset, NOT the
 *     `Z` suffix. `Instant.parse` must accept this format or the entire
 *     series silently becomes empty.
 *  2. The `minimal_response=true` flag makes HA emit a full state dict for
 *     the first row and a slim `{state, last_changed}` shape for the rest.
 *     Both shapes must parse against the same `HaHistoryRow` model.
 *  3. After projection, the resulting Y values must actually vary — a flat
 *     series would render as a flat line regardless of any chart bug.
 */
class HaHistoryPipelineRegressionTest {

    private val realResponse = """
        [[
          {"entity_id":"sensor.temp","state":"6.6","attributes":{},"last_changed":"2026-05-15T07:49:53+00:00","last_updated":"2026-05-15T07:49:53+00:00"},
          {"state":"6.8","last_changed":"2026-05-15T10:26:46.405023+00:00"},
          {"state":"7.5","last_changed":"2026-05-15T12:26:46.405023+00:00"},
          {"state":"8.3","last_changed":"2026-05-15T16:26:46.405023+00:00"},
          {"state":"9.1","last_changed":"2026-05-16T07:41:46.029318+00:00"}
        ]]
    """.trimIndent()

    @Test
    fun `real HA response with plus-offset timestamps produces a varying numeric series`() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val listOfList = Types.newParameterizedType(
            List::class.java,
            Types.newParameterizedType(List::class.java, HaHistoryRow::class.java)
        )
        val rows = moshi.adapter<List<List<HaHistoryRow>>>(listOfList)
            .fromJson(realResponse)!!
            .first()

        assertEquals("all rows should parse, including the slim minimal_response shape",
            5, rows.size)

        val series = HistorySeriesBuilder.build(
            rows = rows,
            domain = "sensor",
            unitOfMeasurement = "°C"
        )

        assertTrue("sensor with a unit should classify as Numeric, was ${series.kind}",
            series.kind is SeriesKind.Numeric)

        assertEquals("every row should project to a point — '+00:00' timestamps must parse",
            5, series.points.size)

        val values = series.points.mapNotNull { it.value }
        assertEquals("every row should have a non-null projected value", 5, values.size)
        assertNotEquals("Y values must actually vary — otherwise the chart looks flat",
            values.min(), values.max())
        assertEquals(6.6, values.first(), 0.0001)
        assertEquals(9.1, values.last(), 0.0001)

        val timestamps = series.points.map { it.timestamp }
        assertEquals("series must be in chronological order",
            timestamps.sorted(), timestamps)
        assertNotNull(timestamps.first())
    }
}
