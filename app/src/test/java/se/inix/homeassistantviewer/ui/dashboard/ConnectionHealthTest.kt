package se.inix.homeassistantviewer.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionHealthTest {

    @Test
    fun `hidden status maps to healthy`() {
        assertEquals(
            ConnectionHealth.Healthy,
            DashboardStatusBar.Hidden.toConnectionHealth()
        )
    }

    @Test
    fun `ready pulse maps to healthy`() {
        assertEquals(
            ConnectionHealth.Healthy,
            DashboardStatusBar.Ready("Connected").toConnectionHealth()
        )
    }

    @Test
    fun `connecting maps to pending`() {
        assertEquals(
            ConnectionHealth.Pending,
            DashboardStatusBar.Connecting("Connecting…").toConnectionHealth()
        )
    }

    @Test
    fun `refreshing maps to pending`() {
        assertEquals(
            ConnectionHealth.Pending,
            DashboardStatusBar.Refreshing("Loading…").toConnectionHealth()
        )
    }

    @Test
    fun `warning maps to unhealthy`() {
        assertEquals(
            ConnectionHealth.Unhealthy,
            DashboardStatusBar.Warning("One offline").toConnectionHealth()
        )
    }

    @Test
    fun `error maps to unhealthy`() {
        assertEquals(
            ConnectionHealth.Unhealthy,
            DashboardStatusBar.Error("Auth failed").toConnectionHealth()
        )
    }
}
