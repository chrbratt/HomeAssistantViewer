package se.inix.homeassistantviewer.data.backup

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Encodes/decodes [AppBackupSnapshot] as UTF-8 JSON for file export and
 * internal snapshots. Pure — no Android types — so round-trips are unit-testable.
 */
class BackupCodec {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(AppBackupSnapshot::class.java)

    fun encode(snapshot: AppBackupSnapshot): ByteArray =
        adapter.toJson(snapshot).encodeToByteArray()

    fun decode(bytes: ByteArray): AppBackupSnapshot {
        val json = bytes.decodeToString()
        return adapter.fromJson(json)
            ?: throw BackupParseException("Backup file is empty or malformed")
    }

    fun decode(json: String): AppBackupSnapshot =
        adapter.fromJson(json) ?: throw BackupParseException("Backup file is empty or malformed")
}

class BackupParseException(message: String) : Exception(message)
