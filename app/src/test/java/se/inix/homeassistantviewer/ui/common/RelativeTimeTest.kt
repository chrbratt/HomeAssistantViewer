package se.inix.homeassistantviewer.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    @Test
    fun `under ten seconds reads now`() {
        assertEquals("now", formatRelativeShort(0))
        assertEquals("now", formatRelativeShort(9))
    }

    @Test
    fun `negative elapsed clamps to now`() {
        assertEquals("now", formatRelativeShort(-5))
    }

    @Test
    fun `seconds below a minute`() {
        assertEquals("10s", formatRelativeShort(10))
        assertEquals("59s", formatRelativeShort(59))
    }

    @Test
    fun `minutes floor below an hour`() {
        assertEquals("1m", formatRelativeShort(60))
        assertEquals("1m", formatRelativeShort(119))
        assertEquals("59m", formatRelativeShort(3_599))
    }

    @Test
    fun `hours floor below a day`() {
        assertEquals("1h", formatRelativeShort(3_600))
        assertEquals("23h", formatRelativeShort(86_399))
    }

    @Test
    fun `days for anything longer`() {
        assertEquals("1d", formatRelativeShort(86_400))
        assertEquals("3d", formatRelativeShort(3 * 86_400))
    }

    @Test
    fun `refresh interval matches granularity`() {
        assertEquals(1_000L, relativeRefreshIntervalMs(0))
        assertEquals(1_000L, relativeRefreshIntervalMs(59))
        assertEquals(30_000L, relativeRefreshIntervalMs(60))
        assertEquals(30_000L, relativeRefreshIntervalMs(3_599))
        assertEquals(300_000L, relativeRefreshIntervalMs(3_600))
    }
}
