package com.cashpilot.android

import com.cashpilot.android.model.MonitoredApp
import com.cashpilot.android.service.Detection
import com.cashpilot.android.ui.AppDisplayInfo
import com.cashpilot.android.ui.AppPresentation
import com.cashpilot.android.ui.AppState
import com.cashpilot.android.ui.FleetSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * What the dashboard shows when it cannot see anything.
 *
 * Fixing the per-app cards was not enough. With every app resolving to
 * [AppState.UNKNOWN], two things still read as confident claims:
 *
 *  * the summary counted only RUNNING and STOPPED, so both were zero and the
 *    header said "0 running" — "nothing is earning", in a different place;
 *  * the grid became eleven identical "Can't tell" cards, which is one sentence
 *    repeated with the actual fix nowhere on screen.
 */
class BlindDashboardTest {

    private fun info(state: AppState, slug: String) =
        AppDisplayInfo(app = MonitoredApp(slug, "pkg.$slug", slug), state = state, status = null)

    // Calls production. The first version of this file re-implemented the
    // counting here, and controls that removed the UNKNOWN tally from the
    // ViewModel -- the exact bug under test -- left it green.
    private fun summarise(apps: List<AppDisplayInfo>) = AppPresentation.summarise(apps)

    @Test
    fun `unknown apps are counted, not swallowed`() {
        // THE BUG. Without an `unknown` field these apps appear in no count at
        // all, and a header reading "0 running, 0 stopped" states that nothing
        // is earning -- on a device where everything may well be.
        val summary = summarise(List(3) { info(AppState.UNKNOWN, "app$it") })
        assertEquals(3, summary.unknown)
        assertEquals(0, summary.running)
        assertEquals(0, summary.stopped)
    }

    @Test
    fun `every app is accounted for in some bucket`() {
        // The invariant that would have caught the original omission: no app may
        // fall between the counts.
        val apps = listOf(
            info(AppState.RUNNING, "a"),
            info(AppState.STOPPED, "b"),
            info(AppState.UNKNOWN, "c"),
            info(AppState.DISABLED, "d"),
            info(AppState.NOT_INSTALLED, "e"),
        )
        val s = summarise(apps)
        assertEquals(apps.size, s.running + s.stopped + s.unknown + s.disabled + s.notInstalled)
    }

    @Test
    fun `a healthy device reports no unknowns`() {
        // The counter must not be permanently non-zero, or it becomes noise the
        // UI hides and nobody reads.
        val s = summarise(listOf(info(AppState.RUNNING, "a"), info(AppState.STOPPED, "b")))
        assertEquals(0, s.unknown)
    }

    @Test
    fun `the summary defaults to zero unknowns`() {
        assertEquals(0, FleetSummary().unknown)
    }

    @Test
    fun `unknown is declared last so positional destructuring is unchanged`() {
        // SystemInfo documents the same rule: DataClassContractTest destructures
        // these classes positionally, so member order is part of the contract.
        // Adding `unknown` third broke that test -- the convention working.
        val s = FleetSummary(running = 1, stopped = 2, notInstalled = 3, disabled = 4, totalTx = 5L, totalRx = 6L)
        val (running, stopped, notInstalled, disabled, tx, rx) = s
        assertEquals(1, running)
        assertEquals(2, stopped)
        assertEquals(3, notInstalled)
        assertEquals(4, disabled)
        assertEquals(5L, tx)
        assertEquals(6L, rx)
    }

    @Test
    fun `blindness is exactly no signal source at all`() {
        // One granted permission is NOT blind: a positive signal from either
        // source is still proof of life, so the grid stays meaningful.
        assertTrue(Detection.isBlind(canSeeNotifications = false, canSeeUsage = false))
        assertFalse(Detection.isBlind(canSeeNotifications = true, canSeeUsage = false))
        assertFalse(Detection.isBlind(canSeeNotifications = false, canSeeUsage = true))
    }

    @Test
    fun `a blind device resolves every app to UNKNOWN`() {
        // The premise of suppressing the grid. If some apps still resolved to a
        // real state while blind, hiding the grid would hide real information.
        val running = Detection.resolveRunning(
            canSeeNotifications = false,
            canSeeUsage = false,
            notificationActive = false,
            recentlyActive = false,
            hasRecentNetworkActivity = false,
        )
        assertEquals(
            AppState.UNKNOWN,
            AppPresentation.resolveState(installed = true, enabled = true, running = running),
        )
    }

    @Test
    fun `unknown apps still sort above the ones that are fine`() {
        val sorted = AppPresentation.sortForDashboard(
            listOf(info(AppState.RUNNING, "r"), info(AppState.UNKNOWN, "u"), info(AppState.STOPPED, "s")),
        )
        assertEquals(listOf("s", "u", "r"), sorted.map { it.app.slug })
    }
}
