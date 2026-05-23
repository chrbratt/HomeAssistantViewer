package se.inix.homeassistantviewer.domain.history

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

const val HISTORY_CSV_MIME_TYPE = "text/csv"
const val HISTORY_CSV_SUFFIX = ".csv"

data class HistoryExportMetadata(
    val exportedAt: Instant,
    val range: HistoryRange,
    val rangeStart: Instant,
    val rangeEnd: Instant
)

data class HistoryEntityExport(
    val connectionId: String,
    val connectionName: String,
    val entityId: String,
    val displayName: String,
    val series: HistorySeries
)

object HistoryCsvEncoder {

    private val instantFormatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

    private val header = listOf(
        "timestamp_utc",
        "connection_id",
        "connection_name",
        "entity_id",
        "display_name",
        "domain",
        "state",
        "numeric_value",
        "unit",
        "series_kind"
    ).joinToString(",")

    fun encode(
        metadata: HistoryExportMetadata,
        entities: List<HistoryEntityExport>
    ): ByteArray {
        val lines = buildList {
            add("# exported_at,${escape(metadata.exportedAt.formatInstant())}")
            add("# range_label,${escape(metadata.range.label)}")
            add("# range_start,${escape(metadata.rangeStart.formatInstant())}")
            add("# range_end,${escape(metadata.rangeEnd.formatInstant())}")
            add("# entity_count,${entities.size}")
            add("")
            add(header)
            entities.forEach { entity ->
                entity.series.points
                    .filter { point -> point.timestamp in metadata.rangeStart..metadata.rangeEnd }
                    .forEach { point ->
                        add(formatRow(entity, point))
                    }
            }
        }
        return lines.joinToString("\n").encodeToByteArray()
    }

    fun rowCount(
        metadata: HistoryExportMetadata,
        entities: List<HistoryEntityExport>
    ): Int = entities.sumOf { entity ->
        entity.series.points.count { point ->
            point.timestamp in metadata.rangeStart..metadata.rangeEnd
        }
    }

    private fun formatRow(
        entity: HistoryEntityExport,
        point: HistoryPoint
    ): String {
        val kind = entity.series.kind
        val unit = when (kind) {
            is SeriesKind.Numeric -> kind.unit.orEmpty()
            else -> ""
        }
        val kindLabel = when (kind) {
            is SeriesKind.Numeric -> "numeric"
            is SeriesKind.Binary -> "binary"
            is SeriesKind.Categorical -> "categorical"
        }
        return listOf(
            escape(point.timestamp.formatInstant()),
            escape(entity.connectionId),
            escape(entity.connectionName),
            escape(entity.entityId),
            escape(entity.displayName),
            escape(entity.entityId.substringBefore('.')),
            escape(point.rawState),
            point.value?.toString().orEmpty(),
            escape(unit),
            escape(kindLabel)
        ).joinToString(",")
    }

    private fun Instant.formatInstant(): String = instantFormatter.format(this)

    internal fun escape(value: String): String {
        if (value.none { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            return value
        }
        return "\"${value.replace("\"", "\"\"")}\""
    }
}

fun suggestHistoryExportFileName(
    prefix: String,
    range: HistoryRange,
    exportedAt: Instant = Instant.now()
): String {
    val date = exportedAt.atZone(ZoneOffset.UTC).toLocalDate()
    val safePrefix = prefix
        .lowercase()
        .replace(Regex("[^a-z0-9._-]+"), "-")
        .trim('-')
        .take(80)
        .ifBlank { "history" }
    val rangeToken = range.label.replace(' ', '-')
    return "$safePrefix-$rangeToken-$date$HISTORY_CSV_SUFFIX"
}

sealed class HistoryExportFeedback {
    data class Success(val message: String) : HistoryExportFeedback()
    data class Error(val message: String) : HistoryExportFeedback()
}
