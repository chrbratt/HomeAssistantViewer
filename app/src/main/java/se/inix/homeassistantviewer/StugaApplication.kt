package se.inix.homeassistantviewer

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import se.inix.homeassistantviewer.di.AppContainer

class StugaApplication : Application() {
    lateinit var container: AppContainer

    /**
     * Process-wide scope used for the background-debounce job. SupervisorJob
     * so a failing disconnect attempt cannot tear down the entire scope.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var disconnectJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // EncryptedSharedPreferences.create() involves key derivation (~300 ms on first launch).
        // Touching the lazy properties on IO ensures SettingsRepository and ConnectionPool
        // are fully initialised before the first ViewModel requests them, eliminating the
        // startup race where clients are not yet ready when fetchInitialSnapshot() runs.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.settingsRepository
            container.connectionPool
        }

        // Foreground/background lifecycle is observed once at process scope so a
        // backgrounded app that resumes after Doze or screen-off triggers an
        // immediate reconnect + REST refetch, regardless of which Activity owns
        // the visible UI.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            // ON_START fires before ON_RESUME, so the dashboard observes the
            // event right as the first frame is drawn — values are refreshed
            // before the user even has a chance to look at stale ones.
            override fun onStart(owner: LifecycleOwner) {
                disconnectJob?.cancel()
                disconnectJob = null
                container.connectionPool.reconnectAll()
                container.appEvents.notifyForeground()
            }

            /**
             * When the process is backgrounded we want to stop receiving
             * `state_changed` WebSocket events so the device doesn't burn
             * CPU/battery/data parsing UI updates the user cannot see. The
             * disconnect is debounced so a quick app-switch (e.g. opening the
             * keyboard or system share sheet) does not cause needless WS
             * teardown + reconnect churn.
             */
            override fun onStop(owner: LifecycleOwner) {
                disconnectJob?.cancel()
                disconnectJob = appScope.launch {
                    delay(BACKGROUND_DISCONNECT_DELAY_MS)
                    container.connectionPool.disconnectAll()
                }
            }
        })
    }

    private companion object {
        /**
         * Grace window after `ON_STOP` before we tear down WebSockets. Long
         * enough to bridge brief app-switches without churn, short enough
         * that genuine background sessions stop pulling data quickly.
         */
        const val BACKGROUND_DISCONNECT_DELAY_MS = 30_000L
    }
}
