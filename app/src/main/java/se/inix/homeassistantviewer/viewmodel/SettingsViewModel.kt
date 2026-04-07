package se.inix.homeassistantviewer.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import se.inix.homeassistantviewer.data.SettingsRepository
import se.inix.homeassistantviewer.data.ThemeMode

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val dashboardColumns: StateFlow<Int> = settingsRepository.dashboardColumns
    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode

    fun saveDashboardColumns(columns: Int) {
        settingsRepository.saveDashboardColumns(columns)
    }

    fun saveThemeMode(mode: ThemeMode) {
        settingsRepository.saveThemeMode(mode)
    }
}
