package se.inix.homeassistantviewer.viewmodel

import androidx.lifecycle.ViewModel
import se.inix.homeassistantviewer.data.SettingsRepository
import se.inix.homeassistantviewer.data.ThemeMode
import kotlinx.coroutines.flow.StateFlow

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
