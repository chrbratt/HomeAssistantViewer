package se.inix.homeassistantviewer.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import se.inix.homeassistantviewer.data.settings.ColorPalette
import se.inix.homeassistantviewer.data.settings.SettingsRepository
import se.inix.homeassistantviewer.data.settings.ThemeMode

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val dashboardColumns: StateFlow<Int> = settingsRepository.dashboardColumns
    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
    val colorPalette: StateFlow<ColorPalette> = settingsRepository.colorPalette

    fun saveDashboardColumns(columns: Int) {
        settingsRepository.saveDashboardColumns(columns)
    }

    fun saveThemeMode(mode: ThemeMode) {
        settingsRepository.saveThemeMode(mode)
    }

    fun saveColorPalette(palette: ColorPalette) {
        settingsRepository.saveColorPalette(palette)
    }
}
