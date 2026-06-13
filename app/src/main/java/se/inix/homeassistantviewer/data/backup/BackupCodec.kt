package se.inix.homeassistantviewer.data.backup

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException

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

    fun decode(bytes: ByteArray): AppBackupSnapshot = decode(bytes.decodeToString())

    fun decode(json: String): AppBackupSnapshot = try {
        adapter.fromJson(json) ?: throw BackupParseException("Backup file is empty or malformed")
    } catch (e: JsonDataException) {
        // Well-formed JSON whose shape doesn't match the snapshot schema.
        throw BackupParseException(e.message ?: "Backup file has an unexpected structure")
    } catch (e: IOException) {
        // Malformed JSON (Moshi's JsonEncodingException) and other read errors.
        throw BackupParseException(e.message ?: "Backup file is empty or malformed")
    }
}

class BackupParseException(message: String) : Exception(message)
