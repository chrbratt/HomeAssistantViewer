package se.inix.homeassistantviewer.data.backup

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import se.inix.homeassistantviewer.data.model.FavoriteItem
import se.inix.homeassistantviewer.data.model.HaConnection
import se.inix.homeassistantviewer.data.settings.ColorPalette
import se.inix.homeassistantviewer.data.settings.Density
import se.inix.homeassistantviewer.data.settings.ThemeMode

/**
 * Complete portable backup of user configuration: HA connections (with tokens),
 * dashboard favourites (order, dividers, custom display names) and UI prefs.
 */
@JsonClass(generateAdapter = true)
data class AppBackupSnapshot(
    @param:Json(name = "formatVersion") val formatVersion: Int = CURRENT_FORMAT_VERSION,
    @param:Json(name = "exportedAt") val exportedAt: String,
    @param:Json(name = "appVersion") val appVersion: String,
    @param:Json(name = "connections") val connections: List<HaConnection>,
    @param:Json(name = "favorites") val favorites: List<FavoriteBackupItem>,
    @param:Json(name = "dashboard") val dashboard: DashboardBackupPrefs,
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

/** JSON representation of one dashboard slot (entity or row-break divider). */
@JsonClass(generateAdapter = true)
data class FavoriteBackupItem(
    @param:Json(name = "type") val type: String,
    @param:Json(name = "connectionId") val connectionId: String? = null,
    @param:Json(name = "entityId") val entityId: String? = null,
    /** User-chosen display name overriding HA's friendly_name; null = use HA name. */
    @param:Json(name = "customName") val customName: String? = null,
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "title") val title: String? = null,
)

@JsonClass(generateAdapter = true)
data class DashboardBackupPrefs(
    @param:Json(name = "columns") val columns: Int,
    @param:Json(name = "themeMode") val themeMode: String,
    @param:Json(name = "colorPalette") val colorPalette: String,
    @param:Json(name = "density") val density: String,
)

fun FavoriteItem.toBackupItem(): FavoriteBackupItem = when (this) {
    is FavoriteItem.Entity -> FavoriteBackupItem(
        type = TYPE_ENTITY,
        connectionId = connectionId,
        entityId = entityId,
        customName = customName
    )
    is FavoriteItem.Divider -> FavoriteBackupItem(
        type = TYPE_DIVIDER,
        id = id,
        title = title
    )
}

fun FavoriteBackupItem.toFavoriteItem(): FavoriteItem? = when (type) {
    TYPE_ENTITY -> {
        val connId = connectionId?.takeIf { it.isNotBlank() } ?: return null
        val entId = entityId?.takeIf { it.isNotBlank() } ?: return null
        FavoriteItem.Entity(
            connectionId = connId,
            entityId = entId,
            customName = customName?.trim()?.takeUnless { it.isEmpty() }
        )
    }
    TYPE_DIVIDER -> {
        val dividerId = id?.takeIf { it.isNotBlank() } ?: return null
        FavoriteItem.Divider(
            id = dividerId,
            title = title?.trim()?.takeUnless { it.isEmpty() }
        )
    }
    else -> null
}

fun dashboardBackupPrefs(
    columns: Int,
    themeMode: ThemeMode,
    colorPalette: ColorPalette,
    density: Density,
): DashboardBackupPrefs = DashboardBackupPrefs(
    columns = columns,
    themeMode = themeMode.name,
    colorPalette = colorPalette.name,
    density = density.name,
)

const val TYPE_ENTITY = "entity"
const val TYPE_DIVIDER = "divider"

const val BACKUP_MIME_TYPE = "application/json"
const val BACKUP_FILE_SUFFIX = ".haviewer.json"
