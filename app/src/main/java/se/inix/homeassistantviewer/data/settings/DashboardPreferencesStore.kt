package se.inix.homeassistantviewer.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Layout + theme preferences for the dashboard. Theme is read synchronously
 * during construction so the very first Compose frame already uses the right
 * dark/light scheme — otherwise the user sees a brief flash on cold start.
 */
internal class DashboardPreferencesStore(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope
) {

    private val _columns = MutableStateFlow(DEFAULT_COLUMNS)
    val columns: StateFlow<Int> = _columns.asStateFlow()

    private val _themeMode = MutableStateFlow(readInitial(KEY_THEME, ThemeMode.SYSTEM, ThemeMode::valueOf))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _colorPalette = MutableStateFlow(
        readInitial(KEY_PALETTE, ColorPalette.DYNAMIC, ColorPalette::valueOf)
    )
    val colorPalette: StateFlow<ColorPalette> = _colorPalette.asStateFlow()

    private val _density = MutableStateFlow(
        readInitial(KEY_DENSITY, Density.COMFORTABLE, Density::valueOf)
    )
    val density: StateFlow<Density> = _density.asStateFlow()

    private val _textContrast = MutableStateFlow(
        readInitial(KEY_TEXT_CONTRAST, TextContrast.BALANCED, TextContrast::valueOf)
    )
    val textContrast: StateFlow<TextContrast> = _textContrast.asStateFlow()

    private val _cardTimestamp = MutableStateFlow(
        readInitial(KEY_CARD_TIMESTAMP, CardTimestamp.NONE, CardTimestamp::valueOf)
    )
    val cardTimestamp: StateFlow<CardTimestamp> = _cardTimestamp.asStateFlow()

    /** Called by [SettingsRepository] when DataStore replays/updates. */
    internal fun onDataStorePayload(prefs: Preferences) {
        _columns.value = prefs[KEY_COLUMNS] ?: DEFAULT_COLUMNS
        _themeMode.value = prefs[KEY_THEME]
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
        _colorPalette.value = prefs[KEY_PALETTE]
            ?.let { runCatching { ColorPalette.valueOf(it) }.getOrNull() }
            ?: ColorPalette.DYNAMIC
        _density.value = prefs[KEY_DENSITY]
            ?.let { runCatching { Density.valueOf(it) }.getOrNull() }
            ?: Density.COMFORTABLE
        _textContrast.value = prefs[KEY_TEXT_CONTRAST]
            ?.let { runCatching { TextContrast.valueOf(it) }.getOrNull() }
            ?: TextContrast.BALANCED
        _cardTimestamp.value = prefs[KEY_CARD_TIMESTAMP]
            ?.let { runCatching { CardTimestamp.valueOf(it) }.getOrNull() }
            ?: CardTimestamp.NONE
    }

    fun saveColumns(columns: Int) {
        require(columns in 1..3) { "Column count must be 1, 2, or 3" }
        _columns.value = columns
        scope.launch { dataStore.edit { it[KEY_COLUMNS] = columns } }
    }

    fun saveThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        scope.launch { dataStore.edit { it[KEY_THEME] = mode.name } }
    }

    fun saveColorPalette(palette: ColorPalette) {
        _colorPalette.value = palette
        scope.launch { dataStore.edit { it[KEY_PALETTE] = palette.name } }
    }

    fun saveDensity(density: Density) {
        _density.value = density
        scope.launch { dataStore.edit { it[KEY_DENSITY] = density.name } }
    }

    fun saveTextContrast(textContrast: TextContrast) {
        _textContrast.value = textContrast
        scope.launch { dataStore.edit { it[KEY_TEXT_CONTRAST] = textContrast.name } }
    }

    fun saveCardTimestamp(cardTimestamp: CardTimestamp) {
        _cardTimestamp.value = cardTimestamp
        scope.launch { dataStore.edit { it[KEY_CARD_TIMESTAMP] = cardTimestamp.name } }
    }

    /** Applies all dashboard prefs in one DataStore write — used by backup restore. */
    fun applyAll(
        columns: Int,
        themeMode: ThemeMode,
        colorPalette: ColorPalette,
        density: Density,
        textContrast: TextContrast,
        cardTimestamp: CardTimestamp
    ) {
        require(columns in 1..3) { "Column count must be 1, 2, or 3" }
        _columns.value = columns
        _themeMode.value = themeMode
        _colorPalette.value = colorPalette
        _density.value = density
        _textContrast.value = textContrast
        _cardTimestamp.value = cardTimestamp
        scope.launch {
            dataStore.edit {
                it[KEY_COLUMNS] = columns
                it[KEY_THEME] = themeMode.name
                it[KEY_PALETTE] = colorPalette.name
                it[KEY_DENSITY] = density.name
                it[KEY_TEXT_CONTRAST] = textContrast.name
                it[KEY_CARD_TIMESTAMP] = cardTimestamp.name
            }
        }
    }

    /**
     * Read the persisted value for [key] synchronously so the very first
     * Compose frame already uses the right scheme — otherwise the user sees
     * a brief flash on cold start.
     */
    private fun <T> readInitial(
        key: androidx.datastore.preferences.core.Preferences.Key<String>,
        default: T,
        parser: (String) -> T
    ): T = runCatching {
        runBlocking(Dispatchers.IO) {
            val prefs = dataStore.data.first()
            prefs[key]?.let(parser) ?: default
        }
    }.getOrDefault(default)

    companion object {
        const val DEFAULT_COLUMNS = 2
        internal val KEY_COLUMNS = intPreferencesKey("columns")
        internal val KEY_THEME   = stringPreferencesKey("theme_mode")
        internal val KEY_PALETTE = stringPreferencesKey("color_palette")
        internal val KEY_DENSITY = stringPreferencesKey("density")
        internal val KEY_TEXT_CONTRAST = stringPreferencesKey("text_contrast")
        internal val KEY_CARD_TIMESTAMP = stringPreferencesKey("card_timestamp")
    }
}
