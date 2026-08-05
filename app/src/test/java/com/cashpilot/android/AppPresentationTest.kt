package com.cashpilot.android

import com.cashpilot.android.model.AppStatus
import com.cashpilot.android.model.MonitoredApp
import com.cashpilot.android.ui.AppDisplayInfo
import com.cashpilot.android.ui.AppPresentation
import com.cashpilot.android.ui.AppState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The dashboard's ordering and state rules — calling the production functions.
 *
 * `AppStateResolutionTest` covers the same state resolution by **copying** the
 * `when`-expression into the test, saying so in its own comment: "this is the
 * exact same when-expression used in production". A copy cannot fail when
 * production changes, so it verified the copy. The logic now lives in
 * [AppPresentation] and both call it.
 */
class AppPresentationTest {

    private fun app(slug: String, name: String = slug) =
        MonitoredApp(slug = slug, packageName = "pkg.$slug", displayName = name)

    private fun info(state: AppState, slug: String, name: String = slug) =
        AppDisplayInfo(app = app(slug, name), state = state, status = null)

    // ---------------------------------------------------------------- state

    @Test
    fun `not installed beats every other signal`() {
        assertEquals(
            AppState.NOT_INSTALLED,
            AppPresentation.resolveState(installed = false, enabled = true, running = true),
        )
    }

    @Test
    fun `a disabled app is not reported as stopped`() {
        // "Stopped" is a problem; "disabled" is a choice. Conflating them would
        // put every app the user switched off at the top of the attention list.
        assertEquals(
            AppState.DISABLED,
            AppPresentation.resolveState(installed = true, enabled = false, running = false),
        )
    }

    @Test
    fun `an undetectable app is UNKNOWN, not stopped`() {
        // I originally wrote this asserting STOPPED, reasoning that silence
        // about a possibly-dead app was the worse failure. That was wrong once
        // the cause was traced: null arises when the PERMISSIONS are denied, so
        // every app on such a device read as STOPPED and the screen stated they
        // had all died. Different cause, different fix -- grant the permission,
        // do not restart the app.
        assertEquals(
            AppState.UNKNOWN,
            AppPresentation.resolveState(installed = true, enabled = true, running = null),
        )
    }

    @Test
    fun `a definite false is still STOPPED`() {
        // The distinction only works if a real negative still reads as a problem.
        assertEquals(
            AppState.STOPPED,
            AppPresentation.resolveState(installed = true, enabled = true, running = false),
        )
    }

    @Test
    fun `unknown ranks below stopped but above running`() {
        val rank = AppPresentation::attentionRank
        assertTrue(rank(AppState.STOPPED) < rank(AppState.UNKNOWN))
        assertTrue(rank(AppState.UNKNOWN) < rank(AppState.RUNNING))
    }

    @Test
    fun `a running app is running`() {
        assertEquals(
            AppState.RUNNING,
            AppPresentation.resolveState(installed = true, enabled = true, running = true),
        )
    }

    // ---------------------------------------------------------------- order

    @Test
    fun `stopped apps sort above running ones`() {
        // THE BUG. The dashboard sorted by AppState.ordinal — the enum's
        // declaration order, which puts RUNNING first — so apps that were fine
        // took the top of the screen and anything STOPPED was pushed below.
        assertTrue(
            AppPresentation.attentionRank(AppState.STOPPED) <
                AppPresentation.attentionRank(AppState.RUNNING),
            "a stopped earner must not be buried under the ones that are fine",
        )
    }

    @Test
    fun `ordering does not depend on the enum declaration order`() {
        // The original defect was sorting by AppState.ordinal. The enum has
        // since been reordered (STOPPED first) for readability, which is exactly
        // why the rank table must stay independent of it -- otherwise a future
        // reorder silently rearranges the screen again. Asserting the ordinals
        // here would re-create that coupling, so this asserts the rank table
        // covers every state instead.
        assertTrue(AppPresentation.rankIsTotal())
        assertEquals(
            AppState.entries.size,
            AppState.entries.map(AppPresentation::attentionRank).distinct().size,
        )
    }

    @Test
    fun `things the user cannot act on sort last`() {
        val rank = AppPresentation::attentionRank
        assertTrue(rank(AppState.RUNNING) < rank(AppState.DISABLED))
        assertTrue(rank(AppState.DISABLED) < rank(AppState.NOT_INSTALLED))
    }

    @Test
    fun `every state has a distinct rank`() {
        // A new AppState given a duplicate rank would make the order depend on
        // the sort's stability rather than on the table.
        assertTrue(AppPresentation.rankIsTotal(), "two states share a rank")
    }

    @Test
    fun `the full dashboard order puts problems first`() {
        val sorted = AppPresentation.sortForDashboard(
            listOf(
                info(AppState.NOT_INSTALLED, "titan"),
                info(AppState.RUNNING, "earnapp"),
                info(AppState.DISABLED, "grass"),
                info(AppState.STOPPED, "iproyal"),
            ),
        )
        assertEquals(
            listOf("iproyal", "earnapp", "grass", "titan"),
            sorted.map { it.app.slug },
        )
    }

    @Test
    fun `apps in the same state keep a stable alphabetical order`() {
        // Without a secondary key the detectors' return order decides, so the
        // grid reshuffles under the user's thumb on every 30-second refresh.
        val once = AppPresentation.sortForDashboard(
            listOf(
                info(AppState.STOPPED, "uprock", "Uprock"),
                info(AppState.STOPPED, "earnapp", "EarnApp"),
                info(AppState.STOPPED, "grass", "Grass"),
            ),
        )
        val again = AppPresentation.sortForDashboard(
            listOf(
                info(AppState.STOPPED, "grass", "Grass"),
                info(AppState.STOPPED, "uprock", "Uprock"),
                info(AppState.STOPPED, "earnapp", "EarnApp"),
            ),
        )
        assertEquals(listOf("earnapp", "grass", "uprock"), once.map { it.app.slug })
        assertEquals(once.map { it.app.slug }, again.map { it.app.slug })
    }

    @Test
    fun `sorting is case-insensitive`() {
        val sorted = AppPresentation.sortForDashboard(
            listOf(
                info(AppState.RUNNING, "b", "beta"),
                info(AppState.RUNNING, "a", "Alpha"),
            ),
        )
        assertEquals(listOf("a", "b"), sorted.map { it.app.slug })
    }

    @Test
    fun `sorting an empty list is not an error`() {
        assertEquals(emptyList<AppDisplayInfo>(), AppPresentation.sortForDashboard(emptyList()))
    }

    @Test
    fun `sorting does not drop or duplicate anything`() {
        val input = listOf(
            info(AppState.RUNNING, "a"),
            info(AppState.STOPPED, "b"),
            info(AppState.STOPPED, "c"),
            info(AppState.NOT_INSTALLED, "d"),
        )
        val sorted = AppPresentation.sortForDashboard(input)
        assertEquals(input.size, sorted.size)
        assertEquals(input.map { it.app.slug }.toSet(), sorted.map { it.app.slug }.toSet())
    }

    @Test
    fun `status is carried through untouched`() {
        // The sort must not rebuild the items and lose what the detectors found.
        val status = AppStatus(slug = "earnapp", running = true)
        val sorted = AppPresentation.sortForDashboard(
            listOf(AppDisplayInfo(app = app("earnapp"), state = AppState.RUNNING, status = status)),
        )
        assertEquals(status, sorted.single().status)
    }
}
