package se.inix.homeassistantviewer.ui.dashboard

/**
 * A user-initiated change to one entity. The view-model dispatches these to the
 * appropriate repository method via [EntityActionDispatcher].
 */
sealed class EntityAction {
    abstract val connectionId: String
    abstract val entityId: String

    data class Toggle(override val connectionId: String, override val entityId: String) : EntityAction()
    data class SetBrightness(override val connectionId: String, override val entityId: String, val pct: Int) : EntityAction()
    data class TurnOff(override val connectionId: String, override val entityId: String) : EntityAction()
    data class OpenCover(override val connectionId: String, override val entityId: String) : EntityAction()
    data class CloseCover(override val connectionId: String, override val entityId: String) : EntityAction()
    data class StopCover(override val connectionId: String, override val entityId: String) : EntityAction()
    data class SetCoverPosition(override val connectionId: String, override val entityId: String, val position: Int) : EntityAction()
    data class SetClimateTemperature(override val connectionId: String, override val entityId: String, val temperature: Double) : EntityAction()
    data class SetClimateHvacMode(override val connectionId: String, override val entityId: String, val mode: String) : EntityAction()
    data class SetFanPercentage(override val connectionId: String, override val entityId: String, val percentage: Int) : EntityAction()
    data class Lock(override val connectionId: String, override val entityId: String) : EntityAction()
    data class Unlock(override val connectionId: String, override val entityId: String) : EntityAction()
    data class MediaPlayPause(override val connectionId: String, override val entityId: String) : EntityAction()
    data class MediaPrevious(override val connectionId: String, override val entityId: String) : EntityAction()
    data class MediaNext(override val connectionId: String, override val entityId: String) : EntityAction()
    data class SetMediaVolume(override val connectionId: String, override val entityId: String, val volume: Float) : EntityAction()
    data class Activate(override val connectionId: String, override val entityId: String) : EntityAction()
    data class TriggerAutomation(override val connectionId: String, override val entityId: String) : EntityAction()
    data class SetInputNumber(override val connectionId: String, override val entityId: String, val value: Double) : EntityAction()
}
