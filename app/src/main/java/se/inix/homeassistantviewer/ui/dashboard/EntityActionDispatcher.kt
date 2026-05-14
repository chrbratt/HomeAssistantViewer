package se.inix.homeassistantviewer.ui.dashboard

import android.util.Log
import se.inix.homeassistantviewer.data.model.HaEntityState
import se.inix.homeassistantviewer.data.ws.ConnectionPool

/**
 * Owns the giant `when (action)` that used to live inline in the view-model.
 *
 * Each handler:
 *  1. applies an optimistic state via [optimistic] so the UI feels instant
 *  2. calls the matching repository method
 *  3. rolls the optimistic state back to [current] on failure
 *
 * Actions that don't have a meaningful intermediate state (StopCover,
 * MediaPrevious/Next, Activate, TriggerAutomation) skip step 1.
 */
internal class EntityActionDispatcher(
    private val connectionPool: ConnectionPool,
    private val readState: (EntityKey) -> HaEntityState?,
    private val optimistic: (EntityKey, HaEntityState) -> Unit
) {

    /** Dispatch one user action. Suspends until the REST call completes. */
    suspend fun dispatch(action: EntityAction) {
        val key = EntityKey(action.connectionId, action.entityId)
        val current = readState(key)
        val repo = connectionPool.repositoryFor(action.connectionId)
        val entityId = action.entityId

        when (action) {
            is EntityAction.Toggle -> {
                current ?: return
                optimistic(key, current.copy(state = if (current.state == "on") "off" else "on"))
                if (repo == null) { optimistic(key, current); return }
                val domain = entityId.substringBefore(".")
                runCatching { repo.callService(domain, "toggle", entityId) }
                    .onFailure {
                        Log.e(TAG, "Toggle failed $entityId", it)
                        optimistic(key, current)
                    }
            }

            is EntityAction.TurnOff -> {
                current ?: return
                repo ?: return
                optimistic(key, current.copy(state = "off"))
                val domain = entityId.substringBefore(".")
                runCatching { repo.callService(domain, "turn_off", entityId) }
                    .onFailure { Log.e(TAG, "TurnOff failed $entityId", it); optimistic(key, current) }
            }

            is EntityAction.SetBrightness -> {
                current ?: return
                repo ?: return
                if (action.pct <= 0) {
                    // 0 % is interpreted as off — avoids HA's ambiguous "on at brightness=0".
                    dispatch(EntityAction.TurnOff(action.connectionId, entityId))
                    return
                }
                val raw = (action.pct / 100.0 * 255.0).toInt().coerceIn(0, 255).toDouble()
                optimistic(key, current.copy(
                    state = "on",
                    attributes = (current.attributes ?: emptyMap()) + ("brightness" to raw)
                ))
                runCatching { repo.setLightBrightness(entityId, action.pct) }
                    .onFailure { Log.e(TAG, "SetBrightness failed $entityId", it); optimistic(key, current) }
            }

            is EntityAction.OpenCover -> {
                repo ?: return
                current?.let { optimistic(key, it.copy(state = "opening")) }
                runCatching { repo.callService("cover", "open_cover", entityId) }
                    .onFailure { Log.e(TAG, "OpenCover failed $entityId", it); current?.let { optimistic(key, it) } }
            }
            is EntityAction.CloseCover -> {
                repo ?: return
                current?.let { optimistic(key, it.copy(state = "closing")) }
                runCatching { repo.callService("cover", "close_cover", entityId) }
                    .onFailure { Log.e(TAG, "CloseCover failed $entityId", it); current?.let { optimistic(key, it) } }
            }
            is EntityAction.StopCover -> {
                repo ?: return
                runCatching { repo.callService("cover", "stop_cover", entityId) }
                    .onFailure { Log.e(TAG, "StopCover failed $entityId", it) }
            }
            is EntityAction.SetCoverPosition -> {
                current ?: return
                repo ?: return
                optimistic(key, current.copy(
                    attributes = (current.attributes ?: emptyMap()) +
                        ("current_position" to action.position.toDouble())
                ))
                runCatching { repo.setCoverPosition(entityId, action.position) }
                    .onFailure { Log.e(TAG, "SetCoverPosition failed $entityId", it); optimistic(key, current) }
            }

            is EntityAction.SetClimateTemperature -> {
                current ?: return
                repo ?: return
                optimistic(key, current.copy(
                    attributes = (current.attributes ?: emptyMap()) + ("temperature" to action.temperature)
                ))
                runCatching { repo.setClimateTemperature(entityId, action.temperature) }
                    .onFailure { Log.e(TAG, "SetClimateTemp failed $entityId", it); optimistic(key, current) }
            }
            is EntityAction.SetClimateHvacMode -> {
                current ?: return
                repo ?: return
                optimistic(key, current.copy(state = action.mode))
                runCatching { repo.setClimateHvacMode(entityId, action.mode) }
                    .onFailure { Log.e(TAG, "SetClimateMode failed $entityId", it); optimistic(key, current) }
            }

            is EntityAction.SetFanPercentage -> {
                current ?: return
                repo ?: return
                optimistic(key, current.copy(
                    attributes = (current.attributes ?: emptyMap()) +
                        ("percentage" to action.percentage.toDouble())
                ))
                runCatching { repo.setFanPercentage(entityId, action.percentage) }
                    .onFailure { Log.e(TAG, "SetFanPct failed $entityId", it); optimistic(key, current) }
            }

            is EntityAction.Lock -> {
                current ?: return
                repo ?: return
                optimistic(key, current.copy(state = "locked"))
                runCatching { repo.lockEntity(entityId) }
                    .onFailure { Log.e(TAG, "Lock failed $entityId", it); optimistic(key, current) }
            }
            is EntityAction.Unlock -> {
                current ?: return
                repo ?: return
                optimistic(key, current.copy(state = "unlocked"))
                runCatching { repo.unlockEntity(entityId) }
                    .onFailure { Log.e(TAG, "Unlock failed $entityId", it); optimistic(key, current) }
            }

            is EntityAction.MediaPlayPause -> {
                repo ?: return
                current?.let {
                    val nextState = when (it.state) {
                        "playing" -> "paused"
                        "paused", "idle", "on" -> "playing"
                        else -> it.state
                    }
                    if (nextState != it.state) optimistic(key, it.copy(state = nextState))
                }
                runCatching { repo.mediaPlayPause(entityId) }
                    .onFailure { Log.e(TAG, "MediaPlayPause failed $entityId", it); current?.let { optimistic(key, it) } }
            }
            is EntityAction.MediaPrevious -> {
                repo ?: return
                runCatching { repo.mediaPreviousTrack(entityId) }
                    .onFailure { Log.e(TAG, "MediaPrev failed $entityId", it) }
            }
            is EntityAction.MediaNext -> {
                repo ?: return
                runCatching { repo.mediaNextTrack(entityId) }
                    .onFailure { Log.e(TAG, "MediaNext failed $entityId", it) }
            }
            is EntityAction.SetMediaVolume -> {
                current ?: return
                repo ?: return
                optimistic(key, current.copy(
                    attributes = (current.attributes ?: emptyMap()) +
                        ("volume_level" to action.volume.toDouble())
                ))
                runCatching { repo.setMediaVolume(entityId, action.volume) }
                    .onFailure { Log.e(TAG, "SetVolume failed $entityId", it); optimistic(key, current) }
            }

            is EntityAction.Activate -> {
                repo ?: return
                val domain = entityId.substringBefore(".")
                runCatching { repo.callService(domain, "turn_on", entityId) }
                    .onFailure { Log.e(TAG, "Activate failed $entityId", it) }
            }

            is EntityAction.TriggerAutomation -> {
                repo ?: return
                runCatching { repo.triggerAutomation(entityId) }
                    .onFailure { Log.e(TAG, "TriggerAutomation failed $entityId", it) }
            }

            is EntityAction.SetInputNumber -> {
                current ?: return
                repo ?: return
                optimistic(key, current.copy(state = action.value.toString()))
                runCatching { repo.setInputNumber(entityId, action.value) }
                    .onFailure { Log.e(TAG, "SetInputNumber failed $entityId", it); optimistic(key, current) }
            }
        }
    }

    companion object {
        private const val TAG = "EntityActionDispatcher"
    }
}
