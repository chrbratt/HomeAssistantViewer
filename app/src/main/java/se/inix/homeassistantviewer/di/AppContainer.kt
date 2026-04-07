package se.inix.homeassistantviewer.di

import android.content.Context
import se.inix.homeassistantviewer.data.ConnectionPool
import se.inix.homeassistantviewer.data.HomeAssistantConnectionTester
import se.inix.homeassistantviewer.data.SettingsRepository

class AppContainer(private val context: Context) {
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    val homeAssistantConnectionTester: HomeAssistantConnectionTester by lazy {
        HomeAssistantConnectionTester()
    }

    /** Singleton pool — one WS + REST client pair per configured connection. */
    val connectionPool: ConnectionPool by lazy {
        ConnectionPool(settingsRepository)
    }
}
