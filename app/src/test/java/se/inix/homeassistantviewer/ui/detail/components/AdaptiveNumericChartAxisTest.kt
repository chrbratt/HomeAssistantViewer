package se.inix.homeassistantviewer.ui.detail.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveNumericChartAxisTest {

    private val plotPoints = ChartPlotPoints(
        minX = 0.0,
        maxX = 100.0,
        points = listOf(
            0.0 to 10.0,
            50.0 to 20.0,
            100.0 to 30.0
        )
    )

    @Test
    fun `full view uses full y span`() {
        val range = computeAdaptiveYRange(
            plotPoints = plotPoints,
            scroll = 0f,
            maxScroll = 0f,
            baselineZoom = 1f,
            currentZoom = 1f
        )
        assertTrue(range.minY < 10.0)
        assertTrue(range.maxY > 30.0)
    }

    @Test
    fun `zoomed view narrows y range to visible window`() {
        val full = computeAdaptiveYRange(
            plotPoints = plotPoints,
            scroll = 0f,
            maxScroll = 0f,
            baselineZoom = 1f,
            currentZoom = 1f
        )
        val zoomed = computeAdaptiveYRange(
            plotPoints = plotPoints,
            scroll = 0f,
            maxScroll = 0f,
            baselineZoom = 1f,
            currentZoom = 4f
        )
        assertTrue(zoomed.maxY - zoomed.minY < full.maxY - full.minY)
    }

    @Test
    fun `format uses fewer decimals for large spans`() {
        assertEquals("21", formatChartAxisValue(21.4, span = 120.0))
        assertEquals("21.5", formatChartAxisValue(21.45, span = 12.0))
        assertEquals("21.45", formatChartAxisValue(21.456, span = 1.2))
    }
}
