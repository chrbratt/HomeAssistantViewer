package se.inix.homeassistantviewer.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import se.inix.homeassistantviewer.StugaApplication

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            DashboardViewModel(
                connectionPool = stugaApplication().container.connectionPool,
                settingsRepository = stugaApplication().container.settingsRepository
            )
        }
        initializer {
            SettingsViewModel(
                settingsRepository = stugaApplication().container.settingsRepository
            )
        }
        initializer {
            EntityPickerViewModel(
                connectionPool = stugaApplication().container.connectionPool,
                settingsRepository = stugaApplication().container.settingsRepository
            )
        }
        initializer {
            ConnectionsViewModel(
                settingsRepository = stugaApplication().container.settingsRepository,
                connectionTester = stugaApplication().container.homeAssistantConnectionTester
            )
        }
    }
}

fun CreationExtras.stugaApplication(): StugaApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StugaApplication)
