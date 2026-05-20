package se.inix.homeassistantviewer.data.backup

import android.content.Context
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * Keeps up to [MAX_SNAPSHOTS] named on-device backups for quick A/B layout
 * switching without using the system file picker.
 */
class InternalSnapshotStore(
    dir: File,
    private val codec: BackupCodec = BackupCodec()
) {
    private val dir = dir.also { it.mkdirs() }

    constructor(context: Context, codec: BackupCodec = BackupCodec()) : this(
        File(context.filesDir, "backups"),
        codec
    )
    private val indexFile = File(dir, INDEX_FILE_NAME)

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val indexAdapter = moshi.adapter<List<SnapshotMeta>>(
        Types.newParameterizedType(List::class.java, SnapshotMeta::class.java)
    )

    fun list(): List<SnapshotMeta> = reconcileIndex().sortedByDescending { it.savedAtEpoch }

    fun load(id: String): AppBackupSnapshot? {
        val file = snapshotFile(id)
        if (!file.exists()) return null
        return runCatching { codec.decode(file.readBytes()) }.getOrNull()
    }

    fun save(name: String, snapshot: AppBackupSnapshot): SnapshotMeta {
        val trimmed = name.trim().ifBlank { "Snapshot" }
        val meta = SnapshotMeta(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            savedAtEpoch = Instant.now().epochSecond
        )
        snapshotFile(meta.id).writeBytes(codec.encode(snapshot))
        val existing = readIndexRaw().filter { snapshotFile(it.id).exists() }
        val pruned = pruneOldest(existing + meta)
        writeIndexAtomic(pruned)
        return meta
    }

    fun delete(id: String) {
        snapshotFile(id).delete()
        writeIndexAtomic(reconcileIndex().filterNot { it.id == id })
    }

    private fun snapshotFile(id: String) = File(dir, "$id$SNAPSHOT_EXT")

    /**
     * Drops index entries whose files are missing and removes orphan snapshot
     * files that are not referenced by the index.
     */
    private fun reconcileIndex(): List<SnapshotMeta> {
        val fromIndex = readIndexRaw().filter { snapshotFile(it.id).exists() }
        val indexedIds = fromIndex.map { it.id }.toSet()

        dir.listFiles()
            ?.filter { file ->
                file.isFile &&
                    file.name.endsWith(SNAPSHOT_EXT) &&
                    file.name != INDEX_FILE_NAME &&
                    !file.name.endsWith(".tmp")
            }
            ?.filter { file ->
                val id = file.name.removeSuffix(SNAPSHOT_EXT)
                id !in indexedIds
            }
            ?.forEach { it.delete() }

        val pruned = fromIndex
        if (pruned != readIndexRaw()) {
            writeIndexAtomic(pruned)
        }
        return pruned
    }

    private fun readIndexRaw(): List<SnapshotMeta> {
        if (!indexFile.exists()) return emptyList()
        return runCatching {
            indexAdapter.fromJson(indexFile.readText()) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    private fun writeIndexAtomic(list: List<SnapshotMeta>) {
        val tmp = File(dir, "$INDEX_FILE_NAME.tmp")
        tmp.writeText(indexAdapter.toJson(list))
        if (indexFile.exists() && !indexFile.delete()) {
            tmp.delete()
            return
        }
        if (!tmp.renameTo(indexFile)) {
            tmp.copyTo(indexFile, overwrite = true)
            tmp.delete()
        }
    }

    private fun pruneOldest(list: List<SnapshotMeta>): List<SnapshotMeta> {
        if (list.size <= MAX_SNAPSHOTS) return list
        val sorted = list.sortedBy { it.savedAtEpoch }
        val toRemove = sorted.take(list.size - MAX_SNAPSHOTS)
        toRemove.forEach { snapshotFile(it.id).delete() }
        return sorted.drop(toRemove.size)
    }

    @JsonClass(generateAdapter = true)
    data class SnapshotMeta(
        @param:Json(name = "id") val id: String,
        @param:Json(name = "name") val name: String,
        @param:Json(name = "savedAtEpoch") val savedAtEpoch: Long
    )

    companion object {
        const val MAX_SNAPSHOTS = 5
        private const val INDEX_FILE_NAME = "index.json"
        private const val SNAPSHOT_EXT = ".json"

        /** Test-only factory — avoids needing a real [Context]. */
        internal fun forTesting(dir: File, codec: BackupCodec = BackupCodec()) =
            InternalSnapshotStore(dir, codec)
    }
}
