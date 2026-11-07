package com.cashpilot.android.redesign

import androidx.compose.runtime.Composable
import com.cashpilot.android.ui.AppState
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The three directions and the screens common to all of them, in dark and light. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE)
class ProposalScreensTest {

    private val healthy = Fixtures.healthy.toHome()
    private val problems = Fixtures.problems.toHome()
    private val standalone = Fixtures.standalone.toHome()
    private val blind = Fixtures.blind.toHome()

    private fun shot(path: String, dark: Boolean = true, content: @Composable () -> Unit) {
        captureRoboImage("$OUT/$path.png") { CpTheme(dark) { content() } }
    }

    private fun direction(id: String, home: @Composable (Home) -> Unit, extra: @Composable () -> Unit) {
        shot("$id/home_healthy") { home(healthy) }
        shot("$id/home_problems") { home(problems) }
        shot("$id/home_standalone") { home(standalone) }
        shot("$id/home_healthy_light", dark = false) { home(healthy) }
        shot("$id/home_problems_light", dark = false) { home(problems) }
        shot("$id/detail") { extra() }
    }

    @Test
    fun directions() {
        renderGate()
        val grass = problems.apps.first { it.slug == "grass" && it.state == AppState.STOPPED }
        direction("a", { AHome(it) }) { AAppDetail(grass, problems) }
        direction("b", { BHome(it) }) { BAddApps(healthy) }
        direction("c", { CHome(it) }) { CApps(problems) }
    }

    @Test
    fun common() {
        renderGate()
        shot("common/onboard_1_welcome") { OnboardWelcome() }
        shot("common/onboard_2_permissions") { OnboardPermissions(notif = true, usage = false, battery = false) }
        shot("common/onboard_2_permissions_light", dark = false) { OnboardPermissions(notif = true, usage = false, battery = false) }
        shot("common/onboard_3_server_ok") { OnboardServer(PairResult.OK) }
        shot("common/onboard_3_server_refused") { OnboardServer(PairResult.KEY_REFUSED) }
        shot("common/blind") { BlindHome(blind) }
        shot("common/settings") { SettingsCommon(problems) }
    }
}
