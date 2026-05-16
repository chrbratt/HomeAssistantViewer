package se.inix.homeassistantviewer.ui.detail.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import se.inix.homeassistantviewer.domain.history.HistoryPoint
import java.time.Instant

/**
 * Tests for the pure helpers behind [BinaryStateTimeline]:
 *  - [computeWindow] — chooses the visible time span (first data point …
 *    max(last data, now)).
 *  - [computeOnIntervals] — turns a sequence of state-changes into ON
 *    intervals that the canvas draws as filled rectangles.
 *
 * These functions encode the only non-trivial logic in the binary chart;
 * once they're right, the canvas drawing is just placement.
 */
class BinaryStateTimelineLogicTest {

    private fun pt(epoch: Long, value: Double?) =
        HistoryPoint(
            timestamp = Instant.ofEpochSecond(epoch),
            value = value,
            rawState = value?.let { if (it >= 0.5) "on" else "off" } ?: "unavailable"
        )

    // --- computeWindow ---------------------------------------------------

    @Test
    fun `computeWindow returns null for empty series`() {
        assertNull(computeWindow(emptyList(), nowEpoch = 1_000L))
    }

    @Test
    fun `computeWindow extends right edge to now when latest data is older`() {
        // Latest point is at t=500; "now" is 1000 → window should reach 1000
        // so a still-ON state draws all the way to the right edge.
        val points = listOf(pt(100, 1.0), pt(500, 1.0))
        val window = computeWindow(points, nowEpoch = 1_000L)
        assertEquals(100L, window?.startEpoch)
        assertEquals(1_000L, window?.endEpoch)
    }

    @Test
    fun `computeWindow uses last data point when it is newer than now`() {
        // Clock skew / future-stamped event — never shrink the chart.
        val points = listOf(pt(100, 1.0), pt(2_000, 0.0))
        val window = computeWindow(points, nowEpoch = 1_000L)
        assertEquals(2_000L, window?.endEpoch)
    }

    // --- computeOnIntervals ----------------------------------------------

    @Test
    fun `computeOnIntervals yields nothing for an empty series`() {
        assertEquals(emptyList<Pair<Long, Long>>(), computeOnIntervals(emptyList(), windowEnd = 1_000L))
    }

    @Test
    fun `single ON point stretches to windowEnd so currently-on switches stay visible`() {
        // Only one row in history: a switch that turned on once and hasn't
        // changed since. The block should reach the right edge.
        val intervals = computeOnIntervals(listOf(pt(100, 1.0)), windowEnd = 1_000L)
        assertEquals(listOf(100L to 1_000L), intervals)
    }

    @Test
    fun `single OFF point produces no intervals — there is nothing to draw`() {
        val intervals = computeOnIntervals(listOf(pt(100, 0.0)), windowEnd = 1_000L)
        assertEquals(emptyList<Pair<Long, Long>>(), intervals)
    }

    @Test
    fun `ON-then-OFF pair becomes one closed interval`() {
        val intervals = computeOnIntervals(
            listOf(pt(100, 1.0), pt(200, 0.0)),
            windowEnd = 1_000L
        )
        assertEquals(listOf(100L to 200L), intervals)
    }

    @Test
    fun `trailing ON state is extended to windowEnd, not to its own timestamp`() {
        // Switch was off, turned on at t=200, still on. The user sees a
        // block from 200 all the way to "now".
        val intervals = computeOnIntervals(
            listOf(pt(100, 0.0), pt(200, 1.0)),
            windowEnd = 1_000L
        )
        assertEquals(listOf(200L to 1_000L), intervals)
    }

    @Test
    fun `null value rows are treated as gaps — no interval emitted for them`() {
        // "unavailable" sandwiched between two ON states does not
        // accidentally fill the gap.
        val intervals = computeOnIntervals(
            listOf(pt(100, 1.0), pt(200, null), pt(300, 1.0), pt(400, 0.0)),
            windowEnd = 1_000L
        )
        // 100→200: ON segment.
        // 200→300: null → skip.
        // 300→400: ON segment.
        // Trailing OFF: nothing extended.
        assertEquals(listOf(100L to 200L, 300L to 400L), intervals)
    }

    @Test
    fun `intervals with zero duration are dropped`() {
        // Two rows at the exact same timestamp shouldn't generate a 0-width
        // block — would just be invisible noise in the rendered output.
        val intervals = computeOnIntervals(
            listOf(pt(100, 1.0), pt(100, 0.0)),
            windowEnd = 1_000L
        )
        assertEquals(emptyList<Pair<Long, Long>>(), intervals)
    }

    // --- applyGesture -----------------------------------------------------

    private val dataWindow = TimelineWindow(startEpoch = 0L, endEpoch = 1_000L)

    @Test
    fun `applyGesture is a no-op when zoom is 1 and pan is 0`() {
        val current = TimelineWindow(200L, 700L)
        val next = applyGesture(
            current = current,
            dataWindow = dataWindow,
            centroidFraction = 0.5f,
            zoom = 1f,
            panFraction = 0f
        )
        assertEquals(current, next)
    }

    @Test
    fun `zooming in around the centre halves the span while keeping the midpoint fixed`() {
        // Pinch-in at centroid 0.5 with zoom=2 → span shrinks from 1000 to
        // 500, anchored at the midpoint (500). New window should be (250, 750).
        val next = applyGesture(
            current = dataWindow,
            dataWindow = dataWindow,
            centroidFraction = 0.5f,
            zoom = 2f,
            panFraction = 0f
        )
        assertEquals(TimelineWindow(250L, 750L), next)
    }

    @Test
    fun `zooming in at the right edge keeps the right edge anchored`() {
        // Pinch at the right edge (cf = 1.0). The time under the fingers
        // (epoch 1000) should remain at the right edge after zoom.
        val next = applyGesture(
            current = dataWindow,
            dataWindow = dataWindow,
            centroidFraction = 1.0f,
            zoom = 2f,
            panFraction = 0f
        )
        assertEquals(1_000L, next.endEpoch)
        assertEquals(500L, next.startEpoch)
    }

    @Test
    fun `dragging right scrolls the viewport backwards in time`() {
        // Already-zoomed-in viewport [400, 600]. Drag right by 50 % of the
        // chart width → viewport shifts left by 50 % of its current span.
        val current = TimelineWindow(400L, 600L)
        val next = applyGesture(
            current = current,
            dataWindow = dataWindow,
            centroidFraction = 0.5f,
            zoom = 1f,
            panFraction = 0.5f
        )
        assertEquals(TimelineWindow(300L, 500L), next)
    }

    @Test
    fun `pan is clamped to the left data boundary`() {
        // Try to scroll past the start: viewport pinned at [0, span] without
        // shrinking, instead of producing negative epochs.
        val current = TimelineWindow(100L, 300L)
        val next = applyGesture(
            current = current,
            dataWindow = dataWindow,
            centroidFraction = 0.5f,
            zoom = 1f,
            panFraction = 1.0f // big right-drag
        )
        assertEquals(0L, next.startEpoch)
        assertEquals(200L, next.endEpoch) // span preserved
    }

    @Test
    fun `pan is clamped to the right data boundary`() {
        val current = TimelineWindow(700L, 900L)
        val next = applyGesture(
            current = current,
            dataWindow = dataWindow,
            centroidFraction = 0.5f,
            zoom = 1f,
            panFraction = -1.0f // big left-drag → scroll forward
        )
        assertEquals(1_000L, next.endEpoch)
        assertEquals(800L, next.startEpoch) // span preserved
    }

    @Test
    fun `zoom-out cannot grow the viewport beyond the data window`() {
        // Already at full data view; pinching out further must not extend
        // the visible window past the data — that would draw blank space.
        val next = applyGesture(
            current = dataWindow,
            dataWindow = dataWindow,
            centroidFraction = 0.5f,
            zoom = 0.5f, // pinch-out
            panFraction = 0f
        )
        assertEquals(dataWindow, next)
    }

    @Test
    fun `zoom-in is clamped to a 60-second minimum span`() {
        // Even at zoom=1000, we should never produce a viewport with span
        // < 60 s — blocks become a single bar and the chart loses meaning.
        val next = applyGesture(
            current = dataWindow,
            dataWindow = dataWindow,
            centroidFraction = 0.5f,
            zoom = 1_000f,
            panFraction = 0f
        )
        assertEquals(60L, next.spanSeconds)
    }
}
