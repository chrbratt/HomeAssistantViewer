package se.inix.homeassistantviewer.ui.detail.components

import org.junit.Assert.assertEquals
import org.junit.Test
import se.inix.homeassistantviewer.domain.history.HistoryPoint
import se.inix.homeassistantviewer.domain.history.StateInterval
import se.inix.homeassistantviewer.domain.history.computeStateIntervals
import java.time.Instant

class CategoricalStateTimelineLogicTest {

    private fun pt(epoch: Long, state: String) = HistoryPoint(
        timestamp = Instant.ofEpochSecond(epoch),
        value = null,
        rawState = state
    )

    @Test
    fun `single state stretches to windowEnd`() {
        val intervals = computeStateIntervals(listOf(pt(100, "sunny")), windowEnd = 500L)
        assertEquals(listOf(StateInterval(100, 500, "sunny")), intervals)
    }

    @Test
    fun `zero-duration transitions are dropped`() {
        val intervals = computeStateIntervals(
            listOf(pt(100, "sunny"), pt(100, "cloudy")),
            windowEnd = 500L
        )
        assertEquals(listOf(StateInterval(100, 500, "cloudy")), intervals)
    }
}
