package se.inix.homeassistantviewer.domain.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DownsamplerTest {

    private fun point(ms: Long, value: Double?): HistoryPoint =
        HistoryPoint(timestamp = Instant.ofEpochMilli(ms), value = value, rawState = value?.toString() ?: "")

    @Test
    fun `input shorter than target is returned untouched`() {
        val input = listOf(point(0, 1.0), point(1000, 2.0), point(2000, 3.0))
        assertEquals(input, Downsampler.downsample(input, 100))
    }

    @Test
    fun `dense series collapses to at most target buckets`() {
        val input = (0 until 5000).map { point(it.toLong() * 100, it.toDouble()) }
        val out = Downsampler.downsample(input, 100)
        assertTrue("expected <= 100, got ${out.size}", out.size <= 100)
    }

    @Test
    fun `downsample preserves monotonic time ordering`() {
        val input = (0 until 1000).map { point(it.toLong(), it.toDouble()) }
        val out = Downsampler.downsample(input, 50)
        var prev = Long.MIN_VALUE
        for (p in out) {
            assertTrue(p.timestamp.toEpochMilli() >= prev)
            prev = p.timestamp.toEpochMilli()
        }
    }

    @Test
    fun `bucket value is the median, not the mean — outliers do not dominate`() {
        // 10 samples in the same bucket window: nine zeros, one outlier
        val input = (0 until 9).map { point(it.toLong(), 0.0) } + point(9, 1000.0)
        val out = Downsampler.downsample(input, 2)
        // First (and likely only) bucket should report the median (0.0), not the mean (~100)
        assertEquals(0.0, out.first().value)
    }

    @Test
    fun `null values are skipped from the median but bucket is still emitted`() {
        val input = listOf(
            point(0, null),
            point(10, 5.0),
            point(20, null),
            point(30, 7.0)
        )
        val out = Downsampler.downsample(input, 2)
        // Each bucket has at least one numeric value -> median should be defined
        out.forEach { assertTrue("bucket value should not be null", it.value != null) }
    }

    @Test
    fun `degenerate range with identical timestamps is returned as-is`() {
        val input = listOf(point(0, 1.0), point(0, 2.0), point(0, 3.0))
        val out = Downsampler.downsample(input, 2)
        assertEquals(input, out)
    }

    @Test
    fun `categorical downsample keeps last state per bucket`() {
        val input = (0 until 100).map {
            HistoryPoint(
                timestamp = Instant.ofEpochSecond(it.toLong()),
                value = null,
                rawState = if (it < 50) "sunny" else "cloudy"
            )
        }
        val out = Downsampler.downsampleCategorical(input, 10)
        assertTrue(out.size <= 10)
        assertEquals("cloudy", out.last().rawState)
    }
}
