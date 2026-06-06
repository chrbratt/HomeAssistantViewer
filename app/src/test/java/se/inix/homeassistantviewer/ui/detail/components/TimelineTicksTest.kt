package se.inix.homeassistantviewer.ui.detail.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Tests for the "nice" time-tick helpers used by every state timeline.
 * The goal is round, stable tick spacing (whole seconds / minutes / hours)
 * regardless of where the visible window happens to start.
 */
class TimelineTicksTest {

    private val utc: ZoneId = ZoneOffset.UTC

    // --- niceTimeStepSeconds ---------------------------------------------

    @Test
    fun `picks whole-minute step for a short window`() {
        // 600 s / 5 = 120 s/tick → exact candidate 120 s (2 min).
        assertEquals(120L, niceTimeStepSeconds(spanSeconds = 600L))
    }

    @Test
    fun `picks a multi-hour step for a quarter-day window`() {
        // 21600 s / 5 = 4320 s → smallest candidate ≥ 4320 is 7200 s (2 h).
        assertEquals(2 * 3600L, niceTimeStepSeconds(spanSeconds = 6 * 3600L))
    }

    @Test
    fun `picks a multi-day step for a week window`() {
        // 604800 s / 5 = 120960 s → smallest candidate ≥ that is 172800 s (2 d).
        assertEquals(2 * 86_400L, niceTimeStepSeconds(spanSeconds = 7 * 86_400L))
    }

    @Test
    fun `tiny window falls back to one-second step`() {
        assertEquals(1L, niceTimeStepSeconds(spanSeconds = 3L))
    }

    // --- niceTimeTicks ----------------------------------------------------

    @Test
    fun `ticks land on round boundaries inside the window`() {
        // Window 00:00:37 .. 00:11:00 (UTC); ticks fall on whole multiples of
        // the chosen step rather than on the arbitrary start epoch.
        val start = 37L
        val end = 660L
        val ticks = niceTimeTicks(start, end, targetTicks = 5, zoneId = utc)
        assertTrue(ticks.isNotEmpty())
        // Every tick is a clean multiple of the chosen step.
        val step = niceTimeStepSeconds(end - start)
        assertTrue(ticks.all { it % step == 0L })
        assertTrue(ticks.all { it in start..end })
    }

    @Test
    fun `ticks are strictly increasing and within bounds`() {
        val ticks = niceTimeTicks(1_000L, 50_000L, targetTicks = 5, zoneId = utc)
        assertTrue(ticks.zipWithNext().all { (a, b) -> b > a })
        assertTrue(ticks.first() >= 1_000L)
        assertTrue(ticks.last() <= 50_000L)
    }

    @Test
    fun `degenerate window returns the start as a single tick`() {
        assertEquals(listOf(500L), niceTimeTicks(500L, 500L, zoneId = utc))
    }

    @Test
    fun `local offset shifts snapping so ticks fall on round local times`() {
        // +02:00 zone: an hourly step should land on local whole hours,
        // i.e. epochs where (epoch + 7200) % 3600 == 0 → epoch % 3600 == 0
        // still holds for whole-hour offsets, so use a +0:30 style check via
        // a fixed offset zone of +05:30.
        val india = ZoneOffset.ofHoursMinutes(5, 30)
        val start = 0L
        val end = 6 * 3600L
        val ticks = niceTimeTicks(start, end, targetTicks = 5, zoneId = india)
        val step = niceTimeStepSeconds(end - start)
        val offset = 5 * 3600L + 30 * 60L
        // Each tick, viewed in local time, is a clean multiple of the step.
        assertTrue(ticks.all { (it + offset) % step == 0L })
    }
}
