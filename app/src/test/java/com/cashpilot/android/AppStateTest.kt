package com.cashpilot.android

import com.cashpilot.android.model.AppStatus
import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.model.MonitoredApp
import com.cashpilot.android.ui.AppDisplayInfo
import com.cashpilot.android.ui.AppPresentation
import com.cashpilot.android.ui.AppState
import com.cashpilot.android.ui.FleetSummary
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AppStateTest {

    @Test
    fun `ordinal is no longer the ordering authority`() {
        // This used to assert "sorting by ordinal puts RUNNING first", which was
        // both the old behaviour AND the bug: it buried stopped apps under the
        // healthy ones. Order now comes from AppPresentation.attentionRank, so
        // that declaration order can be changed without silently rearranging the
        // screen. Asserting the ordinals again here would re-couple them.
        assertTrue(
            AppPresentation.attentionRank(AppState.STOPPED) <
                AppPresentation.attentionRank(AppState.RUNNING),
        )
    }

    @Test
    fun `fleet summary defaults`() {
        val summary = FleetSummary()
        assertEquals(0, summary.running)
        assertEquals(0, summary.stopped)
        assertEquals(0, summary.notInstalled)
        assertEquals(0, summary.disabled)
        assertEquals(0, summary.totalTx)
        assertEquals(0, summary.totalRx)
    }

    @Test
    fun `fleet summary with values`() {
        val summary = FleetSummary(
            running = 3,
            stopped = 2,
            notInstalled = 5,
            disabled = 1,
            totalTx = 1000000,
            totalRx = 5000000,
        )
        assertEquals(3, summary.running)
        assertEquals(5000000, summary.totalRx)
    }

    @Test
    fun `app display info with running state`() {
        val app = MonitoredApp("test", "com.test", "Test App")
        val status = AppStatus(slug = "test", running = true, netTx24h = 100)
        val info = AppDisplayInfo(app = app, state = AppState.RUNNING, status = status)
        assertEquals(AppState.RUNNING, info.state)
        assertEquals(100, info.status?.netTx24h)
    }

    @Test
    fun `app display info with null status`() {
        val app = MonitoredApp("test", "com.test", "Test App")
        val info = AppDisplayInfo(app = app, state = AppState.NOT_INSTALLED)
        assertNull(info.status)
    }

    @Test
    fun `compute fleet summary from display list`() {
        val apps = listOf(
            AppDisplayInfo(
                app = MonitoredApp("a", "com.a", "A"),
                state = AppState.RUNNING,
                status = AppStatus(slug = "a", running = true, netTx24h = 500, netRx24h = 1000),
            ),
            AppDisplayInfo(
                app = MonitoredApp("b", "com.b", "B"),
                state = AppState.RUNNING,
                status = AppStatus(slug = "b", running = true, netTx24h = 300, netRx24h = 700),
            ),
            AppDisplayInfo(
                app = MonitoredApp("c", "com.c", "C"),
                state = AppState.STOPPED,
                status = AppStatus(slug = "c", running = false, netTx24h = 0, netRx24h = 0),
            ),
            AppDisplayInfo(
                app = MonitoredApp("d", "com.d", "D"),
                state = AppState.NOT_INSTALLED,
            ),
        )
        val summary = FleetSummary(
            running = apps.count { it.state == AppState.RUNNING },
            stopped = apps.count { it.state == AppState.STOPPED },
            notInstalled = apps.count { it.state == AppState.NOT_INSTALLED },
            disabled = apps.count { it.state == AppState.DISABLED },
            totalTx = apps.mapNotNull { it.status?.netTx24h }.sum(),
            totalRx = apps.mapNotNull { it.status?.netRx24h }.sum(),
        )
        assertEquals(2, summary.running)
        assertEquals(1, summary.stopped)
        assertEquals(1, summary.notInstalled)
        assertEquals(0, summary.disabled)
        assertEquals(800, summary.totalTx)
        assertEquals(1700, summary.totalRx)
    }

    @Test
    fun `sorting the display list the way the dashboard does`() {
        val list = listOf(
            AppDisplayInfo(app = MonitoredApp("d", "com.d", "D"), state = AppState.DISABLED),
            AppDisplayInfo(app = MonitoredApp("r", "com.r", "R"), state = AppState.RUNNING),
            AppDisplayInfo(app = MonitoredApp("n", "com.n", "N"), state = AppState.NOT_INSTALLED),
            AppDisplayInfo(app = MonitoredApp("s", "com.s", "S"), state = AppState.STOPPED),
        )
        // Calls production instead of re-implementing `sortedBy { ordinal }`,
        // which is what let this test keep asserting RUNNING-first long after
        // the dashboard stopped doing that.
        val sorted = AppPresentation.sortForDashboard(list)
        assertEquals(AppState.STOPPED, sorted[0].state)
        assertEquals(AppState.RUNNING, sorted[1].state)
        assertEquals(AppState.DISABLED, sorted[2].state)
        assertEquals(AppState.NOT_INSTALLED, sorted[3].state)
    }
}
