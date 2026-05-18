package se.inix.homeassistantviewer.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import se.inix.homeassistantviewer.StugaApplication
import se.inix.homeassistantviewer.ui.connections.ConnectionsViewModel
import se.inix.homeassistantviewer.ui.dashboard.DashboardViewModel
import se.inix.homeassistantviewer.ui.picker.EntityPickerViewModel
import se.inix.homeassistantviewer.ui.settings.SettingsViewModel

/**
 * Manual ViewModel factory wiring. Lives alongside [AppContainer] so the
 * DI graph is contained in one package.
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            DashboardViewModel(
                connectionPool = stugaApplication().container.connectionPool,
                settingsRepository = stugaApplication().container.settingsRepository,
                appEvents = stugaApplication().container.appEvents
            )
        }
        initializer {
            SettingsViewModel(
                settingsRepository = stugaApplication().container.settingsRepository,
                backupCodec = stugaApplication().container.backupCodec,
                backupImporter = stugaApplication().container.backupImporter,
                internalSnapshotStore = stugaApplication().container.internalSnapshotStore
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

internal fun CreationExtras.stugaApplication(): StugaApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StugaApplication
