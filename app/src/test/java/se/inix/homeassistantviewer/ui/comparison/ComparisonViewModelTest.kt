package se.inix.homeassistantviewer.ui.comparison

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import se.inix.homeassistantviewer.data.model.ComparisonEntity
import se.inix.homeassistantviewer.data.model.HaEntityState
import se.inix.homeassistantviewer.data.model.HaHistoryRow
import se.inix.homeassistantviewer.domain.history.HistoryRange
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ComparisonViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private open class FakeComparisonDataSource : ComparisonHistoryDataSource {
        val historyCalls = mutableListOf<Triple<String, String, Instant>>()

        override suspend fun getHistory(
            connectionId: String,
            entityId: String,
            start: Instant,
            end: Instant,
            statisticsPeriod: se.inix.homeassistantviewer.domain.history.StatisticsPeriod?
        ): List<HaHistoryRow> {
            historyCalls += Triple(connectionId, entityId, start)
            return listOf(
                HaHistoryRow("20", "2026-01-01T00:00:00Z"),
                HaHistoryRow("22", "2026-01-01T01:00:00Z")
            )
        }

        override suspend fun getCurrentState(
            connectionId: String,
            entityId: String
        ): HaEntityState? = HaEntityState(
            entityId = entityId,
            state = "21",
            attributes = mapOf("unit_of_measurement" to "°C"),
            lastChanged = "2026-01-01T01:00:00Z",
            lastUpdated = "2026-01-01T01:00:00Z"
        )
    }

    @Test
    fun `empty selection exposes EmptySelection state`() = runTest(testDispatcher) {
        val vm = ComparisonViewModel(
            dataSource = FakeComparisonDataSource(),
            comparisonSelection = MutableStateFlow(emptySet()),
            favorites = MutableStateFlow(emptyList()),
            now = { Instant.parse("2026-01-02T00:00:00Z") }
        )
        advanceUntilIdle()
        assertTrue(vm.uiState.value is ComparisonUiState.EmptySelection)
    }

    @Test
    fun `numeric selection loads chart groups`() = runTest(testDispatcher) {
        val entity = ComparisonEntity("c1", "sensor.temp")
        val ds = FakeComparisonDataSource()
        val vm = ComparisonViewModel(
            dataSource = ds,
            comparisonSelection = MutableStateFlow(setOf(entity)),
            favorites = MutableStateFlow(emptyList()),
            now = { Instant.parse("2026-01-02T00:00:00Z") }
        )
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state is ComparisonUiState.Loaded)
        state as ComparisonUiState.Loaded
        assertEquals(HistoryRange.Default, state.range)
        assertEquals(1, state.chartGroups.size)
        assertEquals(1, ds.historyCalls.size)
    }

    @Test
    fun `binary selection loads binary timeline series`() = runTest(testDispatcher) {
        val entity = ComparisonEntity("c1", "switch.kitchen")
        val ds = object : FakeComparisonDataSource() {
            override suspend fun getHistory(
                connectionId: String,
                entityId: String,
                start: Instant,
                end: Instant,
                statisticsPeriod: se.inix.homeassistantviewer.domain.history.StatisticsPeriod?
            ): List<HaHistoryRow> = listOf(
                HaHistoryRow("on", "2026-01-01T00:00:00Z"),
                HaHistoryRow("off", "2026-01-01T01:00:00Z")
            )

            override suspend fun getCurrentState(
                connectionId: String,
                entityId: String
            ): HaEntityState? = HaEntityState(
                entityId = entityId,
                state = "on",
                attributes = emptyMap(),
                lastChanged = "2026-01-01T01:00:00Z",
                lastUpdated = "2026-01-01T01:00:00Z"
            )
        }
        val vm = ComparisonViewModel(
            dataSource = ds,
            comparisonSelection = MutableStateFlow(setOf(entity)),
            favorites = MutableStateFlow(emptyList()),
            now = { Instant.parse("2026-01-02T00:00:00Z") }
        )
        advanceUntilIdle()
        val state = vm.uiState.value as ComparisonUiState.Loaded
        assertTrue(state.chartGroups.isEmpty())
        assertEquals(1, state.binarySeries.size)
        assertTrue(state.excluded.isEmpty())
    }
}
