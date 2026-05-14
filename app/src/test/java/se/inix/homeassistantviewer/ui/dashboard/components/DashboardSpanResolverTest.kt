package se.inix.homeassistantviewer.ui.dashboard.components

import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import org.junit.Assert.assertEquals
import org.junit.Test
import se.inix.homeassistantviewer.ui.dashboard.DashboardItem

class DashboardSpanResolverTest {

    private fun entity(id: String) = DashboardItem.Entity("c", id, null)
    private fun divider(id: String = "d1") = DashboardItem.Divider(id)

    private fun assertFullLine(items: List<DashboardItem>, index: Int) {
        assertEquals(StaggeredGridItemSpan.FullLine, dashboardSpan(items, index))
    }

    private fun assertSingleLane(items: List<DashboardItem>, index: Int) {
        assertEquals(StaggeredGridItemSpan.SingleLane, dashboardSpan(items, index))
    }

    @Test
    fun `single entity in section above divider stretches full line`() {
        val items = listOf(entity("A"), divider(), entity("B"), entity("C"), entity("D"))
        assertFullLine(items, 0)
        assertFullLine(items, 1)
        assertSingleLane(items, 2)
        assertSingleLane(items, 3)
        assertSingleLane(items, 4)
    }

    @Test
    fun `two entities above divider stay single lane`() {
        val items = listOf(entity("A"), entity("B"), divider(), entity("C"))
        assertSingleLane(items, 0)
        assertSingleLane(items, 1)
        assertFullLine(items, 2)
        assertFullLine(items, 3)
    }

    @Test
    fun `single entity below divider stretches full line`() {
        val items = listOf(entity("A"), entity("B"), entity("C"), divider(), entity("D"))
        assertSingleLane(items, 0)
        assertSingleLane(items, 1)
        assertSingleLane(items, 2)
        assertFullLine(items, 3)
        assertFullLine(items, 4)
    }

    @Test
    fun `single entity with no dividers stretches full line`() {
        val items = listOf(entity("A"))
        assertFullLine(items, 0)
    }

    @Test
    fun `several entities with no dividers stay single lane`() {
        val items = listOf(entity("A"), entity("B"), entity("C"))
        assertSingleLane(items, 0)
        assertSingleLane(items, 1)
        assertSingleLane(items, 2)
    }

    @Test
    fun `multiple sections each with one entity all stretch`() {
        val items = listOf(entity("A"), divider("d1"), entity("B"), divider("d2"), entity("C"))
        assertFullLine(items, 0)
        assertFullLine(items, 2)
        assertFullLine(items, 4)
    }

    @Test
    fun `divider at start with single entity after stretches`() {
        val items = listOf(divider(), entity("A"))
        assertFullLine(items, 0)
        assertFullLine(items, 1)
    }

    @Test
    fun `out of bounds index returns single lane`() {
        val items = listOf(entity("A"))
        assertSingleLane(items, -1)
        assertSingleLane(items, 5)
    }

    @Test
    fun `empty list does not crash`() {
        assertSingleLane(emptyList(), 0)
    }
}
