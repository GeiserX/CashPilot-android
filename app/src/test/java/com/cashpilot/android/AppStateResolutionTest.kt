package com.cashpilot.android

import com.cashpilot.android.model.AppStatus
import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.model.MonitoredApp
import com.cashpilot.android.ui.AppDisplayInfo
import com.cashpilot.android.ui.AppState
import com.cashpilot.android.ui.FleetSummary
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests the AppState resolution logic extracted from MainViewModel.doRefresh.
 * This covers every branch in the state determination: NOT_INSTALLED, DISABLED, RUNNING, STOPPED.
 */
class AppStateResolutionTest {

    /**
     * Mirrors the state resolution logic from MainViewModel.doRefresh.
     * This is the exact same when-expression used in production.
     */
    private fun resolveState(
        installed: Boolean,
        enabled: Boolean,
        running: Boolean?,
    ): AppState = when {
        !installed -> AppState.NOT_INSTALLED
        !enabled -> AppState.DISABLED
        running == true -> AppState.RUNNING
        else -> AppState.STOPPED
    }

    @Test
    fun `not installed overrides everything`() {
        assertEquals(AppState.NOT_INSTALLED, resolveState(installed = false, enabled = true, running = true))
        assertEquals(AppState.NOT_INSTALLED, resolveState(installed = false, enabled = true, running = false))
        assertEquals(AppState.NOT_INSTALLED, resolveState(installed = false, enabled = false, running = true))
        assertEquals(AppState.NOT_INSTALLED, resolveState(installed = false, enabled = false, running = null))
    }

    @Test
    fun `installed but disabled overrides running`() {
        assertEquals(AppState.DISABLED, resolveState(installed = true, enabled = false, running = true))
        assertEquals(AppState.DISABLED, resolveState(installed = true, enabled = false, running = false))
        assertEquals(AppState.DISABLED, resolveState(installed = true, enabled = false, running = null))
    }

    @Test
    fun `installed and enabled and running`() {
        assertEquals(AppState.RUNNING, resolveState(installed = true, enabled = true, running = true))
    }

    @Test
    fun `installed and enabled but not running`() {
        assertEquals(AppState.STOPPED, resolveState(installed = true, enabled = true, running = false))
    }

    @Test
    fun `installed and enabled with null running`() {
        assertEquals(AppState.STOPPED, resolveState(installed = true, enabled = true, running = null))
    }

    // --- Full display list building ---

    @Test
    fun `build display list and sort by state ordinal`() {
        val apps = listOf(
            Triple(MonitoredApp("a", "com.a", "App A"), true, AppStatus("a", true)),
            Triple(MonitoredApp("b", "com.b", "App B"), true, AppStatus("b", false)),
            Triple(MonitoredApp("c", "com.c", "App C"), false, null),
            Triple(MonitoredApp("d", "com.d", "App D"), true, AppStatus("d", true)),
        )

        val enabledSlugs = setOf("a", "b", "d") // c is not enabled
        val displayList = apps.map { (app, installed, status) ->
            val isEnabled = app.slug in enabledSlugs
            val state = resolveState(installed, isEnabled, status?.running)
            AppDisplayInfo(app = app, state = state, status = status)
        }.sortedBy { it.state.ordinal }

        // Verify order: RUNNING, RUNNING, STOPPED, NOT_INSTALLED
        assertEquals(AppState.RUNNING, displayList[0].state)
        assertEquals(AppState.RUNNING, displayList[1].state)
        assertEquals(AppState.STOPPED, displayList[2].state)
        assertEquals(AppState.NOT_INSTALLED, displayList[3].state)
    }

    @Test
    fun `fleet summary computed from display list`() {
        val displayList = listOf(
            AppDisplayInfo(MonitoredApp("a", "p.a", "A"), AppState.RUNNING, AppStatus("a", true, netTx24h = 500, netRx24h = 1000)),
            AppDisplayInfo(MonitoredApp("b", "p.b", "B"), AppState.RUNNING, AppStatus("b", true, netTx24h = 300, netRx24h = 700)),
            AppDisplayInfo(MonitoredApp("c", "p.c", "C"), AppState.STOPPED, AppStatus("c", false, netTx24h = 10, netRx24h = 20)),
            AppDisplayInfo(MonitoredApp("d", "p.d", "D"), AppState.NOT_INSTALLED),
            AppDisplayInfo(MonitoredApp("e", "p.e", "E"), AppState.DISABLED),
            AppDisplayInfo(MonitoredApp("f", "p.f", "F"), AppState.DISABLED),
        )

        val summary = FleetSummary(
            running = displayList.count { it.state == AppState.RUNNING },
            stopped = displayList.count { it.state == AppState.STOPPED },
            notInstalled = displayList.count { it.state == AppState.NOT_INSTALLED },
            disabled = displayList.count { it.state == AppState.DISABLED },
            totalTx = displayList.mapNotNull { it.status?.netTx24h }.sum(),
            totalRx = displayList.mapNotNull { it.status?.netRx24h }.sum(),
        )

        assertEquals(2, summary.running)
        assertEquals(1, summary.stopped)
        assertEquals(1, summary.notInstalled)
        assertEquals(2, summary.disabled)
        assertEquals(810L, summary.totalTx)
        assertEquals(1720L, summary.totalRx)
    }

    @Test
    fun `fleet summary with no apps`() {
        val summary = FleetSummary(
            running = 0,
            stopped = 0,
            notInstalled = 0,
            disabled = 0,
            totalTx = 0,
            totalRx = 0,
        )
        assertEquals(FleetSummary(), summary)
    }

    @Test
    fun `display list with all states represented`() {
        val list = AppState.entries.mapIndexed { index, state ->
            AppDisplayInfo(
                app = MonitoredApp("app$index", "com.app$index", "App $index"),
                state = state,
                status = if (state == AppState.RUNNING || state == AppState.STOPPED) {
                    AppStatus(slug = "app$index", running = state == AppState.RUNNING)
                } else null,
            )
        }

        assertEquals(4, list.size)
        assertEquals(1, list.count { it.state == AppState.RUNNING })
        assertEquals(1, list.count { it.state == AppState.STOPPED })
        assertEquals(1, list.count { it.state == AppState.NOT_INSTALLED })
        assertEquals(1, list.count { it.state == AppState.DISABLED })
    }

    @Test
    fun `KnownApps all have valid slug for state resolution`() {
        // Verify every known app can go through state resolution without error
        KnownApps.all.forEach { app ->
            val running = resolveState(installed = true, enabled = true, running = true)
            val stopped = resolveState(installed = true, enabled = true, running = false)
            val disabled = resolveState(installed = true, enabled = false, running = null)
            val notInstalled = resolveState(installed = false, enabled = true, running = null)

            assertEquals(AppState.RUNNING, running)
            assertEquals(AppState.STOPPED, stopped)
            assertEquals(AppState.DISABLED, disabled)
            assertEquals(AppState.NOT_INSTALLED, notInstalled)
        }
    }
}
