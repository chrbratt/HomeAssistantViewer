package se.inix.homeassistantviewer

import android.app.Application
import se.inix.homeassistantviewer.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StugaApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // EncryptedSharedPreferences.create() involves key derivation (~300 ms on first launch).
        // Touching both lazy properties on IO ensures SettingsRepository and ConnectionPool
        // are fully initialised before the first ViewModel requests them, eliminating the
        // startup race where clients are not yet ready when fetchInitialSnapshot() runs.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.settingsRepository
            container.connectionPool
        }
    }
}
