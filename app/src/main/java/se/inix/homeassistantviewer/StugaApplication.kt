package se.inix.homeassistantviewer

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import se.inix.homeassistantviewer.di.AppContainer

class StugaApplication : Application() {
    lateinit var container: AppContainer

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
                container.connectionPool.reconnectAll()
                container.appEvents.notifyForeground()
            }
        })
    }
}
