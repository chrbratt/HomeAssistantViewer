package se.inix.homeassistantviewer.data.ha

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import se.inix.homeassistantviewer.data.model.HaHistoryRow

class HaHistoryParsingTest {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val type = Types.newParameterizedType(
        List::class.java,
        Types.newParameterizedType(List::class.java, HaHistoryRow::class.java)
    )
    private val adapter = moshi.adapter<List<List<HaHistoryRow>>>(type)

    @Test
    fun `parses HA minimal_response shape with one entity`() {
        val json = """
            [
              [
                {"state": "off", "last_changed": "2026-01-01T00:00:00.000Z"},
                {"state": "on",  "last_changed": "2026-01-01T01:15:00.000Z"}
              ]
            ]
        """.trimIndent()
        val result = adapter.fromJson(json)!!
        assertEquals(1, result.size)
        assertEquals(2, result.first().size)
        assertEquals("off", result.first().first().state)
        assertEquals("2026-01-01T01:15:00.000Z", result.first()[1].lastChanged)
    }

    @Test
    fun `parses empty timeline as empty inner list`() {
        val result = adapter.fromJson("[[]]")!!
        assertEquals(1, result.size)
        assertEquals(emptyList<HaHistoryRow>(), result.first())
    }

    @Test
    fun `parses entirely empty outer array`() {
        val result = adapter.fromJson("[]")!!
        assertEquals(emptyList<List<HaHistoryRow>>(), result)
    }

    @Test
    fun `parses unavailable state without throwing`() {
        val json = """[[{"state":"unavailable","last_changed":"2026-01-01T00:00:00Z"}]]"""
        val result = adapter.fromJson(json)!!
        assertEquals("unavailable", result.first().first().state)
    }

    @Test
    fun `parses row with JSON null state (recorder gap) without throwing`() {
        // Real-world HA recorder occasionally logs a row with `state: null`
        // when a state transition was incomplete. Before this fix the whole
        // history fetch failed with Moshi's "Non-Null value 'state' was null"
        // and the chart could not render at all for that entity.
        val json = """
            [[
              {"state": "on",   "last_changed": "2026-01-01T00:00:00Z"},
              {"state": null,   "last_changed": "2026-01-01T00:01:00Z"},
              {"state": "off",  "last_changed": "2026-01-01T00:02:00Z"}
            ]]
        """.trimIndent()
        val rows = adapter.fromJson(json)!!.first()
        assertEquals(3, rows.size)
        assertEquals("on", rows[0].state)
        assertEquals(null, rows[1].state)
        assertEquals("off", rows[2].state)
    }
}
