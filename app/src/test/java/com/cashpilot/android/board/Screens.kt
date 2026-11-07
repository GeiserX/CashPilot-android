package com.cashpilot.android.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.cashpilot.android.board.Situations.NOW
import com.cashpilot.android.ui.AppState
import com.cashpilot.android.ui.board.Access
import com.cashpilot.android.ui.board.AppDetailScreen
import com.cashpilot.android.ui.board.Board
import com.cashpilot.android.ui.board.BoardActions
import com.cashpilot.android.ui.board.Demo
import com.cashpilot.android.ui.board.DemoFrame
import com.cashpilot.android.ui.board.DisclosureScreen
import com.cashpilot.android.ui.board.HomeScreen
import com.cashpilot.android.ui.board.OnboardPermissions
import com.cashpilot.android.ui.board.OnboardServer
import com.cashpilot.android.ui.board.OnboardWelcome
import com.cashpilot.android.ui.board.PairResult
import com.cashpilot.android.ui.board.ServerForm
import com.cashpilot.android.ui.board.SettingsActions
import com.cashpilot.android.ui.board.SettingsScreen
import com.cashpilot.android.ui.board.SupportedAppsScreen
import com.cashpilot.android.ui.theme.CashPilotTheme

/** A phone-sized canvas: 411 x 891 dp at xhdpi (822 x 1782 px). */
const val PHONE = "w411dp-h891dp-xhdpi"

/** One screen to photograph and to check for accessibility. */
class Shot(val name: String, val dark: Boolean, val fontScale: Float = 1f, val content: @Composable () -> Unit) {
    val file get() = "src/test/screenshots/screens/$name.png"
}

@Composable
fun Themed(shot: Shot) {
    CashPilotTheme(darkTheme = shot.dark) {
        if (shot.fontScale == 1f) {
            shot.content()
        } else {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, shot.fontScale)) { shot.content() }
        }
    }
}

/**
 * Every screen of the app, in each situation it can be in, dark and light.
 * The same list drives the goldens and the accessibility checks, so a screen
 * cannot be photographed without also being checked.
 */
object Screens {
    private const val VERSION = "1.0.0"
    private const val DEVICE = "Google Pixel 8"
    private val none = BoardActions()

    /** The row a user is most likely to tap: a stopped app, else the shared one, else the first. */
    private fun detailRow(b: Board) =
        b.watched.firstOrNull { it.state == AppState.STOPPED }
            ?: b.watched.firstOrNull { it.shared }
            ?: b.watched.first()

    private fun form(b: Board, result: PairResult = PairResult.NONE) =
        if (b.paired) ServerForm("https://cashpilot.home.example", "example-fleet-k-7b9a", "example-fleet-k-7b9a", result) else ServerForm("", "", "", result)

    private fun both(name: String, content: @Composable () -> Unit) =
        listOf(Shot("${name}_dark", true, content = content), Shot("${name}_light", false, content = content))

    val all: List<Shot> = buildList {
        for ((s, b) in Situations.four) {
            addAll(both("home_$s") { HomeScreen(b, none) })
            addAll(both("detail_$s") { AppDetailScreen(detailRow(b), b, none) {} })
            addAll(both("settings_$s") { SettingsScreen(b, form(b), VERSION, DEVICE, none, SettingsActions()) {} })
            addAll(both("supported_$s") { SupportedAppsScreen(b, none) {} })
            addAll(both("onboard_2_permissions_$s") { OnboardPermissions(b.notif, b.usage, b.battery, {}, {}, {}) {} })
        }
        addAll(both("onboard_1_welcome") { OnboardWelcome {} })
        for (r in listOf(PairResult.NONE, PairResult.OK, PairResult.KEY_REFUSED, PairResult.UNREACHABLE)) {
            val (url, key) = if (r == PairResult.NONE) "" to "" else "https://cashpilot.home.example" to "example-fleet-k-7b9a"
            addAll(both("onboard_3_server_${r.name.lowercase()}") { OnboardServer(url, key, {}, {}, r, DEVICE, {}, {}, {}) })
        }
        addAll(both("disclose_notifications") { DisclosureScreen(Access.NOTIFICATIONS, {}, {}) })
        addAll(both("disclose_usage") { DisclosureScreen(Access.USAGE, {}, {}) })
        addAll(both("home_no_apps") { HomeScreen(Situations.empty, none) })
        addAll(both("home_demo") { DemoFrame({}) { HomeScreen(Demo.board(NOW), none) } })
        // Font scale 1.3: the screens with the most text, so a layout that only
        // fits at the default size shows up here first.
        add(Shot("font130_home_problems_dark", true, 1.3f) { HomeScreen(Situations.problems, none) })
        add(Shot("font130_onboard_2_permissions_dark", true, 1.3f) { OnboardPermissions(false, false, false, {}, {}, {}) {} })
        add(Shot("font130_settings_healthy_light", false, 1.3f) { SettingsScreen(Situations.healthy, form(Situations.healthy), VERSION, DEVICE, none, SettingsActions()) {} })
    }
}
