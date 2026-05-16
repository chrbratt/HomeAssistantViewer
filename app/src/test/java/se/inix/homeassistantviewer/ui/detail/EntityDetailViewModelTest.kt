package se.inix.homeassistantviewer.ui.detail

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import se.inix.homeassistantviewer.data.model.HaEntityState
import se.inix.homeassistantviewer.data.model.HaHistoryRow
import se.inix.homeassistantviewer.domain.history.HistoryRange
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class EntityDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private class FakeDataSource(
        var historyByCall: List<HaHistoryRow> = emptyList(),
        var currentState: HaEntityState? = null,
        var throwHistory: Boolean = false
    ) : EntityHistoryDataSource {
        val historyCalls = mutableListOf<Triple<String, Instant, Instant>>()
        val live = MutableSharedFlow<HaEntityState>(extraBufferCapacity = 4)
        override val stateChanges: Flow<HaEntityState> get() = live

        override suspend fun getHistory(
            entityId: String, start: Instant, end: Instant
        ): List<HaHistoryRow> {
            historyCalls += Triple(entityId, start, end)
            if (throwHistory) error("network down")
            return historyByCall
        }
        override suspend fun getCurrentState(entityId: String): HaEntityState? = currentState
    }

    private fun handle(connectionId: String = "c1", entityId: String = "switch.kitchen") =
        SavedStateHandle(
            mapOf(
                EntityDetailViewModel.ARG_CONNECTION_ID to connectionId,
                EntityDetailViewModel.ARG_ENTITY_ID to entityId
            )
        )

    @Test
    fun `initial fetch loads the default range and exposes Loaded state`() = runTest(testDispatcher) {
        val ds = FakeDataSource(
            historyByCall = listOf(
                HaHistoryRow("on", "2026-01-01T00:00:00Z"),
                HaHistoryRow("off", "2026-01-01T01:00:00Z")
            ),
            currentState = HaEntityState("switch.kitchen", "off", emptyMap(),
                "2026-01-01T01:00:00Z", "2026-01-01T01:00:00Z")
        )
        val vm = EntityDetailViewModel(handle(), ds, now = { Instant.parse("2026-01-02T00:00:00Z") })

        advanceUntilIdle()

        assertEquals(HistoryRange.Default, vm.selectedRange.value)
        val state = vm.uiState.value
        assertTrue("expected Loaded, was $state", state is EntityDetailUiState.Loaded)
    }

    @Test
    fun `switching range triggers a new history fetch`() = runTest(testDispatcher) {
        val ds = FakeDataSource(
            historyByCall = listOf(HaHistoryRow("on", "2026-01-01T00:00:00Z")),
            currentState = HaEntityState("switch.kitchen", "on", emptyMap(),
                "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z")
        )
        val vm = EntityDetailViewModel(handle(), ds, now = { Instant.parse("2026-01-02T00:00:00Z") })
        advanceUntilIdle()
        val initialCalls = ds.historyCalls.size

        vm.selectRange(HistoryRange.Week)
        advanceUntilIdle()

        assertEquals(initialCalls + 1, ds.historyCalls.size)
        assertEquals(HistoryRange.Week, vm.selectedRange.value)
    }

    @Test
    fun `switching to an already selected range is a no-op`() = runTest(testDispatcher) {
        val ds = FakeDataSource(currentState = HaEntityState(
            "switch.kitchen", "on", emptyMap(),
            "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z"
        ))
        val vm = EntityDetailViewModel(handle(), ds)
        advanceUntilIdle()
        val before = ds.historyCalls.size

        vm.selectRange(HistoryRange.Default)
        advanceUntilIdle()

        assertEquals("no extra fetch expected", before, ds.historyCalls.size)
    }

    @Test
    fun `cached range does not re-fetch when re-selected`() = runTest(testDispatcher) {
        val ds = FakeDataSource(
            historyByCall = listOf(HaHistoryRow("on", "2026-01-01T00:00:00Z")),
            currentState = HaEntityState("switch.kitchen", "on", emptyMap(),
                "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z")
        )
        val vm = EntityDetailViewModel(handle(), ds)
        advanceUntilIdle()
        vm.selectRange(HistoryRange.Hour); advanceUntilIdle()
        vm.selectRange(HistoryRange.Default); advanceUntilIdle()
        val before = ds.historyCalls.size

        vm.selectRange(HistoryRange.Hour); advanceUntilIdle()

        assertEquals("Hour range should be served from cache", before, ds.historyCalls.size)
    }

    @Test
    fun `empty history maps to Empty state, not Error`() = runTest(testDispatcher) {
        val ds = FakeDataSource(
            historyByCall = emptyList(),
            currentState = HaEntityState("switch.kitchen", "on", emptyMap(),
                "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z")
        )
        val vm = EntityDetailViewModel(handle(), ds)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is EntityDetailUiState.Empty)
    }

    @Test
    fun `repository error surfaces as Error state`() = runTest(testDispatcher) {
        val ds = FakeDataSource(throwHistory = true)
        val vm = EntityDetailViewModel(handle(), ds)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue("expected Error, was $state", state is EntityDetailUiState.Error)
    }

    @Test
    fun `live state change for the same entity updates current value in Loaded state`() =
        runTest(testDispatcher) {
            val ds = FakeDataSource(
                // Two rows so the series has the >=2 plottable points required
                // for a Loaded state — a single point can't form a line.
                historyByCall = listOf(
                    HaHistoryRow("on", "2026-01-01T00:00:00Z"),
                    HaHistoryRow("off", "2026-01-01T01:00:00Z")
                ),
                currentState = HaEntityState("switch.kitchen", "on", emptyMap(),
                    "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z")
            )
            val vm = EntityDetailViewModel(handle(), ds)
            advanceUntilIdle()

            val update = HaEntityState("switch.kitchen", "off", emptyMap(),
                "2026-01-01T02:00:00Z", "2026-01-01T02:00:00Z")
            ds.live.emit(update)
            advanceUntilIdle()

            val state = vm.uiState.value as EntityDetailUiState.Loaded
            assertEquals("off", state.currentState?.state)
        }

    @Test
    fun `live state change for a different entity is ignored`() = runTest(testDispatcher) {
        val ds = FakeDataSource(
            historyByCall = listOf(
                HaHistoryRow("on", "2026-01-01T00:00:00Z"),
                HaHistoryRow("off", "2026-01-01T01:00:00Z")
            ),
            currentState = HaEntityState("switch.kitchen", "on", emptyMap(),
                "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z")
        )
        val vm = EntityDetailViewModel(handle(), ds)
        advanceUntilIdle()

        ds.live.emit(HaEntityState("switch.bedroom", "off", emptyMap(),
            "2026-01-01T02:00:00Z", "2026-01-01T02:00:00Z"))
        advanceUntilIdle()

        val state = vm.uiState.value as EntityDetailUiState.Loaded
        assertEquals("on", state.currentState?.state)
    }

    @Test
    fun `live state change appends a new point to the loaded series`() = runTest(testDispatcher) {
        val ds = FakeDataSource(
            historyByCall = listOf(
                HaHistoryRow("21.0", "2026-01-01T00:00:00Z"),
                HaHistoryRow("21.5", "2026-01-01T01:00:00Z")
            ),
            currentState = HaEntityState(
                "sensor.kitchen_temp", "21.5", mapOf("unit_of_measurement" to "°C"),
                "2026-01-01T01:00:00Z", "2026-01-01T01:00:00Z"
            )
        )
        val vm = EntityDetailViewModel(handle(entityId = "sensor.kitchen_temp"), ds)
        advanceUntilIdle()
        val pointsBefore = (vm.uiState.value as EntityDetailUiState.Loaded).series.points.size

        ds.live.emit(
            HaEntityState(
                "sensor.kitchen_temp", "22.0", mapOf("unit_of_measurement" to "°C"),
                "2026-01-01T02:00:00Z", "2026-01-01T02:00:00Z"
            )
        )
        advanceUntilIdle()

        val loaded = vm.uiState.value as EntityDetailUiState.Loaded
        assertEquals("new live state should append one point",
            pointsBefore + 1, loaded.series.points.size)
        assertEquals("22.0", loaded.currentState?.state)
        assertEquals("the appended point should carry the projected numeric value",
            22.0, loaded.series.points.last().value!!, 0.0001)
    }

    @Test
    fun `live state change older than the rightmost point is not appended`() =
        runTest(testDispatcher) {
            val ds = FakeDataSource(
                historyByCall = listOf(
                    HaHistoryRow("21.0", "2026-01-01T00:00:00Z"),
                    HaHistoryRow("21.5", "2026-01-01T02:00:00Z")
                ),
                currentState = HaEntityState(
                    "sensor.kitchen_temp", "21.5", mapOf("unit_of_measurement" to "°C"),
                    "2026-01-01T02:00:00Z", "2026-01-01T02:00:00Z"
                )
            )
            val vm = EntityDetailViewModel(handle(entityId = "sensor.kitchen_temp"), ds)
            advanceUntilIdle()
            val pointsBefore = (vm.uiState.value as EntityDetailUiState.Loaded).series.points.size

            // Older than the rightmost point — should NOT append.
            ds.live.emit(
                HaEntityState(
                    "sensor.kitchen_temp", "21.2", mapOf("unit_of_measurement" to "°C"),
                    "2026-01-01T01:30:00Z", "2026-01-01T01:30:00Z"
                )
            )
            advanceUntilIdle()

            val loaded = vm.uiState.value as EntityDetailUiState.Loaded
            assertEquals("series size must not change for stale events",
                pointsBefore, loaded.series.points.size)
            // Current value still updates so the headline reflects the latest push.
            assertEquals("21.2", loaded.currentState?.state)
        }

    @Test
    fun `series with only unavailable rows is reported as Empty, not Loaded`() =
        runTest(testDispatcher) {
            val ds = FakeDataSource(
                historyByCall = listOf(
                    HaHistoryRow("unavailable", "2026-01-01T00:00:00Z"),
                    HaHistoryRow("unknown", "2026-01-01T01:00:00Z")
                ),
                currentState = HaEntityState("sensor.kitchen_temp", "unavailable", emptyMap(),
                    "2026-01-01T01:00:00Z", "2026-01-01T01:00:00Z")
            )
            val vm = EntityDetailViewModel(
                handle(entityId = "sensor.kitchen_temp"),
                ds
            )
            advanceUntilIdle()

            assertTrue(
                "expected Empty for all-unavailable series, was ${vm.uiState.value}",
                vm.uiState.value is EntityDetailUiState.Empty
            )
        }

    @Test
    fun `customName mirrors the source flow`() = runTest(testDispatcher) {
        val ds = FakeDataSource()
        val source = MutableStateFlow<String?>("Kitchen lamp")
        val vm = EntityDetailViewModel(
            savedStateHandle = handle(),
            dataSource = ds,
            customNameSource = source
        )
        // VM uses `stateIn(WhileSubscribed)`, so we need an active subscriber
        // to mirror what `collectAsStateWithLifecycle` does in production.
        val collector = launch { vm.customName.collect {} }
        advanceUntilIdle()
        assertEquals("Kitchen lamp", vm.customName.value)

        source.value = "Counter light"
        advanceUntilIdle()
        assertEquals("Counter light", vm.customName.value)

        source.value = null
        advanceUntilIdle()
        assertNull(vm.customName.value)
        collector.cancel()
    }

    @Test
    fun `setCustomName trims whitespace and persists a non-blank name`() =
        runTest(testDispatcher) {
            val ds = FakeDataSource()
            var saved: String? = "untouched"
            val vm = EntityDetailViewModel(
                savedStateHandle = handle(),
                dataSource = ds,
                saveCustomName = { saved = it }
            )

            vm.setCustomName("   Hallway light   ")

            assertEquals("Hallway light", saved)
        }

    @Test
    fun `setCustomName persists null for blank or whitespace-only input`() =
        runTest(testDispatcher) {
            val ds = FakeDataSource()
            var saved: String? = "untouched"
            val vm = EntityDetailViewModel(
                savedStateHandle = handle(),
                dataSource = ds,
                saveCustomName = { saved = it }
            )

            vm.setCustomName("")
            assertNull("empty string should clear", saved)

            saved = "untouched"
            vm.setCustomName("   ")
            assertNull("whitespace-only should clear", saved)

            saved = "untouched"
            vm.setCustomName(null)
            assertNull("null should clear", saved)
        }
}
