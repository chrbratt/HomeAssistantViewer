package se.inix.homeassistantviewer.di

import android.content.Context
import okhttp3.OkHttpClient
import se.inix.homeassistantviewer.data.events.AppEvents
import se.inix.homeassistantviewer.data.ha.HomeAssistantConnectionTester
import se.inix.homeassistantviewer.data.settings.SettingsRepository
import se.inix.homeassistantviewer.data.ws.ConnectionPool
import java.util.concurrent.TimeUnit

/**
 * Manual dependency-injection root. Each property is the single shared instance
 * for that concern; everything else (view-models, screens) reaches them through
 * `(application as StugaApplication).container`.
 */
class AppContainer(private val context: Context) {

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    /** Cross-cutting event bus (e.g. process foreground transitions). */
    val appEvents: AppEvents by lazy { AppEvents() }

    /**
     * Shared base [OkHttpClient]. Per-connection clients are derived via
     * [OkHttpClient.newBuilder] so they share dispatcher and connection pool —
     * recreating a connection no longer leaks a thread pool.
     *
     * Timeouts apply both to REST and to WebSocket upgrade. The ping interval
     * is set here because WebSocket clients are built from this same instance.
     */
    val sharedOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    }

    val homeAssistantConnectionTester: HomeAssistantConnectionTester by lazy {
        HomeAssistantConnectionTester(sharedOkHttpClient)
    }

    /** Singleton pool — one WS + REST client pair per configured connection. */
    val connectionPool: ConnectionPool by lazy {
        ConnectionPool(settingsRepository, sharedOkHttpClient)
    }
}
