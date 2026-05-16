package se.inix.homeassistantviewer.domain.history

/**
 * Reduces a long history series to a target point count for rendering.
 *
 * Strategy: time-bucket the input into [targetPoints] equal slices and
 * emit one [HistoryPoint] per non-empty bucket carrying the median of
 * the bucket's values. Median (over mean) is more robust to single
 * outlier readings without losing the shape of the curve.
 *
 * For binary or discrete series the projected values are 0/1/null, so
 * the median naturally collapses to the dominant state in each bucket,
 * preserving step-chart fidelity at low resolutions.
 *
 * The first and last bucket-anchored points are always kept so the chart
 * does not visually shrink the time window.
 */
internal object Downsampler {

    /**
     * @param points chronologically ordered input series.
     * @param targetPoints upper bound on the returned size. Must be > 1.
     */
    fun downsample(points: List<HistoryPoint>, targetPoints: Int): List<HistoryPoint> {
        require(targetPoints > 1) { "targetPoints must be > 1, was $targetPoints" }
        if (points.size <= targetPoints) return points
        val first = points.first().timestamp.toEpochMilli()
        val last = points.last().timestamp.toEpochMilli()
        if (last <= first) return points

        val bucketWidthMs = ((last - first).toDouble() / targetPoints).coerceAtLeast(1.0)
        val buckets = Array<MutableList<HistoryPoint>?>(targetPoints) { null }

        for (p in points) {
            val offset = p.timestamp.toEpochMilli() - first
            val index = (offset / bucketWidthMs).toInt().coerceIn(0, targetPoints - 1)
            val bucket = buckets[index] ?: mutableListOf<HistoryPoint>().also { buckets[index] = it }
            bucket.add(p)
        }

        return buckets.mapNotNull { bucket ->
            if (bucket.isNullOrEmpty()) return@mapNotNull null
            val midTimestamp = bucket[bucket.size / 2].timestamp
            val medianValue = bucket.mapNotNull { it.value }.sorted().let { sorted ->
                when {
                    sorted.isEmpty() -> null
                    sorted.size % 2 == 1 -> sorted[sorted.size / 2]
                    else -> (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
                }
            }
            // Carry through the raw state of the middle observation so the
            // tooltip / accessibility text can still describe what was seen.
            val midRaw = bucket[bucket.size / 2].rawState
            HistoryPoint(timestamp = midTimestamp, value = medianValue, rawState = midRaw)
        }
    }
}
