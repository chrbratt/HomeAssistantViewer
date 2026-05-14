package se.inix.homeassistantviewer.data.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Cross-cutting application events that view-models react to but which
 * originate outside of the UI layer (e.g. from a process-lifecycle observer).
 *
 * Kept intentionally tiny — one event per concern — so view-models stay
 * decoupled from `Application` and `Activity` callbacks.
 */
class AppEvents {
    private val _foregroundEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    /** Emitted every time the process transitions from background to foreground. */
    val foregroundEvents: SharedFlow<Unit> = _foregroundEvents.asSharedFlow()

    fun notifyForeground() {
        _foregroundEvents.tryEmit(Unit)
    }
}
